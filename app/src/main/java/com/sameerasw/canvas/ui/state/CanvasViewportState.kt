package com.sameerasw.canvas.ui.state

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * The camera over the infinite canvas: a uniform [scale] plus a translation in screen pixels.
 *
 * screen = world * scale + offset
 */
@Stable
class CanvasViewportState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    /** True when the view is anywhere other than the home position (100%, not panned). */
    val isTransformed: Boolean by derivedStateOf {
        abs(scale - 1f) > 0.005f || abs(offsetX) > 0.5f || abs(offsetY) > 0.5f
    }

    /** Zoom level as a whole percentage, for display. */
    val zoomPercent: Int by derivedStateOf { (scale * 100f).toInt() }

    fun toWorld(screen: Offset): Offset =
        Offset((screen.x - offsetX) / scale, (screen.y - offsetY) / scale)

    fun toScreen(world: Offset): Offset =
        Offset(world.x * scale + offsetX, world.y * scale + offsetY)

    fun panBy(delta: Offset) {
        offsetX += delta.x
        offsetY += delta.y
    }

    /**
     * Apply one step of a pinch: zoom by [zoom] around [centroid] (the world point under the
     * fingers stays under the fingers), then move by [pan]. Zoom is 1:1 with finger spread.
     */
    fun transform(centroid: Offset, pan: Offset, zoom: Float) {
        val oldScale = scale
        val newScale = (oldScale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val worldX = (centroid.x - offsetX) / oldScale
        val worldY = (centroid.y - offsetY) / oldScale
        scale = newScale
        offsetX = centroid.x - worldX * newScale + pan.x
        offsetY = centroid.y - worldY * newScale + pan.y
    }

    /** Spring back to 100% with no pan. */
    suspend fun animateReset() {
        val fromScale = scale
        val fromX = offsetX
        val fromY = offsetY
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { t, _ ->
            scale = fromScale + (1f - fromScale) * t
            offsetX = fromX * (1f - t)
            offsetY = fromY * (1f - t)
        }
    }

    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 10f
    }
}

@Composable
fun rememberCanvasViewportState(): CanvasViewportState = remember { CanvasViewportState() }
