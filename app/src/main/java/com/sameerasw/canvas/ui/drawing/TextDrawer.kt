package com.sameerasw.canvas.ui.drawing

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.canvas.R
import com.sameerasw.canvas.data.TextItem

object TextDrawer {
    @Volatile
    private var cachedTypeface: Typeface? = null
    private var typefaceLoaded = false

    private fun typeface(context: Context): Typeface? {
        if (!typefaceLoaded) {
            cachedTypeface = try {
                ResourcesCompat.getFont(context, R.font.font)
            } catch (_: Exception) {
                null
            }
            typefaceLoaded = true
        }
        return cachedTypeface
    }

    private fun paintFor(context: Context, size: Float, colorInt: Int = 0) = Paint().apply {
        color = colorInt
        textSize = if (size <= 0f) 16f else size
        isAntiAlias = true
        typeface(context)?.let { typeface = it }
    }

    fun DrawScope.drawStringWithFont(context: Context, text: String, x: Float, y: Float, fontSize: Float, colorInt: Int) {
        if (text.isEmpty()) return
        val paint = paintFor(context, fontSize, colorInt)
        drawIntoCanvas { canvas ->
            val fm = paint.fontMetrics
            val baseline = y - fm.ascent
            canvas.nativeCanvas.drawText(text, x, baseline, paint)
        }
    }

    /** World-space bounds of a text item, as drawn by [drawStringWithFont]. */
    fun bounds(context: Context, item: TextItem): Rect {
        val paint = paintFor(context, item.size)
        val fm = paint.fontMetrics
        val width = paint.measureText(item.text).coerceAtLeast(item.size * 0.5f)
        val height = (fm.descent - fm.ascent).coerceAtLeast(item.size)
        return Rect(item.x, item.y, item.x + width, item.y + height)
    }
}
