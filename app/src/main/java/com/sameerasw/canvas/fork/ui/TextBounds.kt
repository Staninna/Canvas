package com.sameerasw.canvas.fork.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Rect
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.canvas.R
import com.sameerasw.canvas.data.TextItem

/**
 * World-space bounds of a text item as upstream's TextDrawer.drawStringWithFont draws it
 * (top-left at x/y, same font), for hit-testing.
 */
object TextBounds {
    @Volatile
    private var typeface: Typeface? = null
    private var loaded = false

    fun of(context: Context, item: TextItem): Rect {
        if (!loaded) {
            typeface = try { ResourcesCompat.getFont(context, R.font.font) } catch (_: Exception) { null }
            loaded = true
        }
        val paint = Paint().apply {
            textSize = if (item.size <= 0f) 16f else item.size
            isAntiAlias = true
            typeface?.let { this.typeface = it }
        }
        val fm = paint.fontMetrics
        val width = paint.measureText(item.text).coerceAtLeast(item.size * 0.5f)
        val height = (fm.descent - fm.ascent).coerceAtLeast(item.size)
        return Rect(item.x, item.y, item.x + width, item.y + height)
    }
}
