package com.sameerasw.canvas.fork

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.JsonParser
import com.sameerasw.canvas.data.CanvasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Keeps canvas background images in filesDir/backgrounds/, where imported .canvas backgrounds
 * already live, so they are part of app backups (Seedvault). A content:// grant from the photo
 * picker does not survive a restore; a copy in app storage does.
 *
 * Stored values stay absolute file:// URIs like upstream's import path, because every reader
 * (upstream export helpers included) opens them with ContentResolver.
 */
object BackgroundStore {
    private const val TAG = "BackgroundStore"
    private const val DIR = "backgrounds"

    @Volatile private var cleanedThisProcess = false

    private fun dir(context: Context) = File(context.filesDir, DIR)

    /** Copies [source] into backgrounds/ and returns its file:// URI, or null on failure. */
    suspend fun copyIn(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = dir(context).apply { mkdirs() }
            val file = File(dir, "bg_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(source)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            } ?: return@withContext null
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not copy background $source", e)
            null
        }
    }

    /**
     * Returns the URI a stored background should have, or null when it is fine as it is.
     * - content:// (picked before this change): copied in while the grant still works.
     * - file:// into backgrounds/ that no longer exists (restored into another user profile,
     *   so filesDir moved): pointed at the same file name under this filesDir.
     */
    suspend fun migrate(context: Context, stored: String): String? {
        val uri = stored.toUri()
        return when (uri.scheme) {
            "content" -> copyIn(context, uri)
            "file" -> withContext(Dispatchers.IO) {
                val path = uri.path ?: return@withContext null
                val parent = File(path).parentFile?.name
                if (File(path).exists() || parent != DIR) return@withContext null
                val local = File(dir(context), File(path).name)
                if (local.exists()) Uri.fromFile(local).toString() else null
            }
            else -> null
        }
    }

    /**
     * Once per process, deletes background files the saved canvas no longer references.
     * Runs at startup, before any undo history exists, and only touches files older than
     * the start of the run so a background picked meanwhile is never removed.
     */
    suspend fun cleanUpOnce(context: Context) = withContext(Dispatchers.IO) {
        if (cleanedThisProcess) return@withContext
        cleanedThisProcess = true
        try {
            val cutoff = System.currentTimeMillis()
            val json = CanvasRepository(context).loadCanvas() ?: return@withContext
            val obj = JsonParser.parseString(json)
            if (!obj.isJsonObject) return@withContext
            val bg = obj.asJsonObject.get("backgroundImageUri")
            val keep = if (bg == null || bg.isJsonNull) null else bg.asString.toUri().lastPathSegment
            dir(context).listFiles()?.forEach { file ->
                if (file.name != keep && file.lastModified() < cutoff) file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Background cleanup skipped", e)
        }
    }
}
