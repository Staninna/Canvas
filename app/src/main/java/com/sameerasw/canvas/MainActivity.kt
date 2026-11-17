package com.sameerasw.canvas

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.sameerasw.canvas.ui.CanvasApp
import com.sameerasw.canvas.ui.theme.CanvasTheme
import com.sameerasw.canvas.util.ContentCaptureManager
import com.sameerasw.canvas.util.KeyguardHelper
import com.sameerasw.canvas.util.NotesRoleManager

class MainActivity : ComponentActivity() {
    private val viewModel: CanvasViewModel by viewModels()

    // ActivityResultLauncher for Notes Role request
    private val notesRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("MainActivity", "Notes Role granted successfully")
            // Refresh Notes role status
            val isNotesRoleHeld = NotesRoleManager.isNotesRoleHeld(this)
            viewModel.setIsNotesRoleHeld(isNotesRoleHeld)
            Toast.makeText(this, "Notes Role granted!", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("MainActivity", "Notes Role request denied or cancelled")
            Toast.makeText(this, "Notes Role request cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // ActivityResultLauncher for content capture
    private val contentCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        ContentCaptureManager.handleCaptureResult(
            resultCode = result.resultCode,
            data = result.data,
            onSuccess = { uri ->
                Log.d("MainActivity", "Content captured: $uri")
                Toast.makeText(this, "Content captured successfully", Toast.LENGTH_SHORT).show()
                // TODO: Handle pasting captured image into canvas
            },
            onFailed = {
                Toast.makeText(this, "Content capture failed", Toast.LENGTH_SHORT).show()
            },
            onUserCanceled = {
                Log.d("MainActivity", "User cancelled content capture")
            },
            onWindowModeUnsupported = {
                Toast.makeText(
                    this,
                    "Content capture only available in floating window",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onBlockedByAdmin = {
                Toast.makeText(this, "Content capture blocked by administrator", Toast.LENGTH_SHORT)
                    .show()
            }
        )
    }

    // ActivityResultLauncher for picking background image
    private val pickBackgroundImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            Log.d("MainActivity", "Background image selected: $uri")
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to take persistable URI permission: ${e.message}")
            }
            viewModel.setBackgroundImageUri(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init settings repository early so HapticUtil can read preferences
        SettingsRepository.init(this)

        // Handle Notes Role intent
        handleNotesRoleIntent(intent)

        // Check and update Notes Role status
        val isNotesRoleHeld = NotesRoleManager.isNotesRoleHeld(this)
        viewModel.setIsNotesRoleHeld(isNotesRoleHeld)
        Log.d("MainActivity", "Notes Role held: $isNotesRoleHeld")

        enableEdgeToEdge()
        setContent {
            CanvasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CanvasApp(
                        viewModel = viewModel,
                        onPickBackground = {
                            pickBackgroundImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRequestNotesRole = { requestNotesRole() },
                        onLaunchContentCapture = { launchContentCapture() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onPause() {
        super.onPause()
        viewModel.save()
    }

    override fun onStop() {
        super.onStop()
        viewModel.save()
    }

    /**
     * Handle new intents delivered to the activity.
     * This includes ACTION_CREATE_NOTE intents when the app is already running.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotesRoleIntent(intent)
    }

    /**
     * Handles intents related to the Notes Role feature.
     * Extracts stylus mode hint and lock screen status from the intent.
     */
    private fun handleNotesRoleIntent(intent: Intent) {
        // Check if this is an ACTION_CREATE_NOTE intent
        if (intent.action == Intent.ACTION_CREATE_NOTE) {
            Log.d("MainActivity", "Received ACTION_CREATE_NOTE intent")

            // Check if stylus mode is requested
            val useStylusMode = NotesRoleManager.shouldUseStylusMode(intent)
            viewModel.setStylusMode(useStylusMode)
            Log.d("MainActivity", "Stylus mode: $useStylusMode")

            // Check if launched from lock screen
            val launchedFromLockScreen = KeyguardHelper.isKeyguardLocked(this)
            viewModel.setLaunchedFromLockScreen(launchedFromLockScreen)
            Log.d("MainActivity", "Launched from lock screen: $launchedFromLockScreen")

            // If locked and device is secure, request unlock
            if (launchedFromLockScreen && KeyguardHelper.isKeyguardSecure(this)) {
                KeyguardHelper.requestDismissKeyguard(
                    this,
                    onDismissSucceeded = {
                        Log.d("MainActivity", "Device unlocked successfully")
                    },
                    onDismissError = {
                        Log.w("MainActivity", "Failed to unlock device")
                    }
                )
            }
        }

        // Update floating window status
        val isFloating = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                isLaunchedFromBubble
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
        viewModel.setIsFloatingWindow(isFloating)
    }

    /**
     * Request the Notes Role for this app.
     * Call this from a UI button or during app initialization.
     */
    fun requestNotesRole() {
        if (NotesRoleManager.isNotesRoleAvailable(this)) {
            NotesRoleManager.requestNotesRole(this, notesRoleLauncher)
        } else {
            Toast.makeText(this, "Notes Role not available on this device", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Launch content capture for note annotation.
     * Should only be called when in floating window mode.
     */
    fun launchContentCapture() {
        if (!NotesRoleManager.isRunningInFloatingWindow(this)) {
            Toast.makeText(
                this,
                "Content capture only available in floating window",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (ContentCaptureManager.isScreenCaptureDisabled(this)) {
            Toast.makeText(this, "Screen capture disabled by administrator", Toast.LENGTH_SHORT)
                .show()
            return
        }

        ContentCaptureManager.launchContentCapture(contentCaptureLauncher)
    }
}
