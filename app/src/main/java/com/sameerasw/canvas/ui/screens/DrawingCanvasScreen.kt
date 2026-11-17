package com.sameerasw.canvas.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import com.sameerasw.canvas.SettingsRepository
import com.sameerasw.canvas.data.TextItem
import com.sameerasw.canvas.model.DrawStroke
import com.sameerasw.canvas.model.PenStyle
import com.sameerasw.canvas.model.ShapeType
import com.sameerasw.canvas.model.StylusPoint
import com.sameerasw.canvas.model.ToolType
import com.sameerasw.canvas.ui.drawing.BackgroundDrawer
import com.sameerasw.canvas.ui.drawing.StrokeDrawer
import com.sameerasw.canvas.ui.drawing.StrokeDrawer.drawPressureSensitiveStroke
import com.sameerasw.canvas.ui.drawing.StrokeDrawer.drawScribbleStroke
import com.sameerasw.canvas.ui.drawing.TextDrawer
import com.sameerasw.canvas.ui.drawing.TextDrawer.drawStringWithFont
import com.sameerasw.canvas.ui.state.CanvasViewportState
import com.sameerasw.canvas.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/** What the current touch sequence turned out to be. */
private enum class GestureMode {
    /** Drawing with the active tool (pen, arrow, shape, eraser). */
    DRAW,

    /** One finger moving the canvas (hand tool, or dragging empty space with the text tool). */
    PAN,

    /** Two or more fingers: pinch to zoom and pan together. */
    TRANSFORM,

    /** Text tool, finger down but not moved yet: becomes a tap, a text drag or a pan. */
    TEXT_PENDING,

    /** Moving an existing text item. */
    TEXT_DRAG,

    /** The rest of a gesture we are done with (e.g. a palm resting after the stylus lifted). */
    IGNORE
}

/** Taps with two or three fingers shorter than this undo/redo. */
private const val MULTI_FINGER_TAP_TIMEOUT_MS = 300L

/**
 * The drawing surface.
 *
 * Gestures, all handled by one detector so they never fight each other:
 * - one finger / stylus: use the current tool (a tap with the pen leaves a dot)
 * - two fingers: pinch to zoom around the fingers and pan; a quick two-finger tap undoes
 * - three-finger tap: redo
 * - a second finger landing while a finger stroke is in progress cancels that stroke and zooms
 *   instead; while drawing with a stylus, extra touches (a resting palm) are ignored
 * - text tool: tap to add or edit text, drag a text item to move it, drag elsewhere to pan
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvasScreen(
    viewport: CanvasViewportState,
    currentTool: ToolType,
    strokes: List<DrawStroke>,
    texts: List<TextItem>,
    penWidth: Float,
    currentColor: Color,
    currentPenStyle: PenStyle,
    currentShapeType: ShapeType,
    arrowWidth: Float,
    shapeWidth: Float,
    shapeFilled: Boolean,
    eraserRadiusPx: Float,
    canvasBackground: SettingsRepository.CanvasBackgroundType,
    fingerGesturesEnabled: Boolean,
    backgroundImageUri: String?,
    modifier: Modifier = Modifier,
    onAddStroke: (DrawStroke) -> Unit,
    onRemoveStrokes: (recordUndo: Boolean, predicate: (DrawStroke) -> Boolean) -> Boolean,
    onRemoveText: (id: Long, recordUndo: Boolean) -> Boolean,
    onTextTap: (world: Offset, hitTextId: Long?) -> Unit,
    onMoveText: (id: Long, x: Float, y: Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDrawingChange: (Boolean) -> Unit,
    onStylusButtonPressed: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The gesture loop is started once; it reads the latest values through these.
    val tool by rememberUpdatedState(currentTool)
    val strokeColor by rememberUpdatedState(currentColor)
    val penWidthNow by rememberUpdatedState(penWidth)
    val penStyle by rememberUpdatedState(currentPenStyle)
    val shapeType by rememberUpdatedState(currentShapeType)
    val arrowWidthNow by rememberUpdatedState(arrowWidth)
    val shapeWidthNow by rememberUpdatedState(shapeWidth)
    val shapeFilledNow by rememberUpdatedState(shapeFilled)
    val eraserRadius by rememberUpdatedState(eraserRadiusPx)
    val textsNow by rememberUpdatedState(texts)
    val fingerGestures by rememberUpdatedState(fingerGesturesEnabled)
    val addStroke by rememberUpdatedState(onAddStroke)
    val removeStrokes by rememberUpdatedState(onRemoveStrokes)
    val removeText by rememberUpdatedState(onRemoveText)
    val textTap by rememberUpdatedState(onTextTap)
    val moveText by rememberUpdatedState(onMoveText)
    val undo by rememberUpdatedState(onUndo)
    val redo by rememberUpdatedState(onRedo)
    val drawingChange by rememberUpdatedState(onDrawingChange)
    val stylusButton by rememberUpdatedState(onStylusButtonPressed)

    // In-progress stroke, in world coordinates
    val currentStroke = remember { mutableStateListOf<Offset>() }
    val currentStylusPoints = remember { mutableStateListOf<StylusPoint>() }
    var strokeIsStylus by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf<ToolType?>(null) }
    var eraserCursor by remember { mutableStateOf<Offset?>(null) }
    var draggingTextId by remember { mutableStateOf<Long?>(null) }
    var textDragDelta by remember { mutableStateOf(Offset.Zero) }
    val stylusButtonDown = remember { mutableStateOf(false) }

    // Background image
    var bgImage by remember(backgroundImageUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(backgroundImageUri) {
        val uriString = backgroundImageUri ?: run { bgImage = null; return@LaunchedEffect }
        try {
            bgImage = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uriString.toUri())?.use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }?.asImageBitmap()
            }
        } catch (e: Exception) {
            android.util.Log.e("DrawingCanvasScreen", "Error loading background image", e)
        }
    }

    fun hitText(world: Offset, slop: Float): TextItem? =
        textsNow.lastOrNull { TextDrawer.bounds(context, it).inflate(slop).contains(world) }

    fun strokeHit(stroke: DrawStroke, world: Offset, threshold: Float): Boolean {
        val pts = stroke.points
        if (pts.isEmpty()) return false
        if ((stroke.isArrow || stroke.shapeType != null) && pts.size >= 2) {
            val a = pts.first()
            val b = pts.last()
            return world.x in (minOf(a.x, b.x) - threshold)..(maxOf(a.x, b.x) + threshold) &&
                world.y in (minOf(a.y, b.y) - threshold)..(maxOf(a.y, b.y) + threshold)
        }
        if (pts.size == 1) return hypot(world.x - pts[0].x, world.y - pts[0].y) < threshold
        for (i in 0 until pts.size - 1) {
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val lenSq = dx * dx + dy * dy
            val t = if (lenSq == 0f) 0f else
                (((world.x - p1.x) * dx + (world.y - p1.y) * dy) / lenSq).coerceIn(0f, 1f)
            if (hypot(world.x - (p1.x + t * dx), world.y - (p1.y + t * dy)) < threshold) return true
        }
        return false
    }

    Canvas(
        modifier = modifier
            .pointerInteropFilter { event ->
                // S Pen side button: toggles pen/eraser, and erases while held
                if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                    val pressed = (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                    val was = stylusButtonDown.value
                    stylusButtonDown.value = pressed
                    if (pressed && !was) stylusButton()
                }
                false // never consume: the gesture detector below still sees everything
            }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                    val effectiveTool = when {
                        down.type == PointerType.Eraser || stylusButtonDown.value -> ToolType.ERASER
                        else -> tool
                    }
                    val downWorld = viewport.toWorld(down.position)
                    val primaryId = down.id
                    val startTime = down.uptimeMillis
                    var lastTime = startTime
                    var maxPointers = 1
                    var travel = 0f
                    var transformTravel = 0f
                    var drawingNotified = false
                    var erasedThisGesture = false
                    var hapticJob: Job? = null
                    var speed = 0f
                    var lastSpeedTime = startTime
                    var lastSpeedPos = down.position
                    val textHit = if (effectiveTool == ToolType.TEXT) {
                        hitText(downWorld, slop / viewport.scale)
                    } else null

                    fun eraseAt(screen: Offset) {
                        eraserCursor = screen
                        val world = viewport.toWorld(screen)
                        val threshold = eraserRadius / viewport.scale
                        // Only the first removal in a gesture records an undo step
                        var removed = removeStrokes(!erasedThisGesture) { strokeHit(it, world, threshold) }
                        if (removed) erasedThisGesture = true
                        textsNow.filter {
                            TextDrawer.bounds(context, it).inflate(threshold).contains(world)
                        }.forEach {
                            if (removeText(it.id, !erasedThisGesture)) {
                                erasedThisGesture = true
                                removed = true
                            }
                        }
                        if (removed) HapticUtil.performFadeOut(haptics)
                    }

                    fun addPoint(change: PointerInputChange, position: Offset) {
                        val world = viewport.toWorld(position)
                        currentStroke.add(world)
                        if (isStylus) {
                            currentStylusPoints.add(
                                StylusPoint(world, change.pressure.coerceIn(0f, 1f), 0f, 0f)
                            )
                        }
                    }

                    fun clearStroke() {
                        currentStroke.clear()
                        currentStylusPoints.clear()
                        activeTool = null
                        eraserCursor = null
                    }

                    fun commitStroke() {
                        val points = currentStroke.toList()
                        val stylusPts = currentStylusPoints.toList()
                        val moved = travel > slop
                        val color = strokeColor
                        when (activeTool) {
                            ToolType.PEN -> {
                                val pts = if (moved && points.size >= 2) points else
                                    listOf(downWorld, Offset(downWorld.x + 0.5f, downWorld.y + 0.5f))
                                addStroke(
                                    DrawStroke(
                                        pts,
                                        color,
                                        width = penWidthNow,
                                        style = penStyle,
                                        stylusPoints = if (isStylus && moved && stylusPts.size >= 2) stylusPts else null,
                                        isFromStylus = isStylus && moved
                                    )
                                )
                                if (!moved) HapticUtil.performClick(haptics)
                            }

                            ToolType.ARROW -> if (moved && points.size >= 2) {
                                addStroke(
                                    DrawStroke(
                                        listOf(points.first(), points.last()),
                                        color,
                                        width = arrowWidthNow,
                                        isArrow = true,
                                        stylusPoints = if (isStylus && stylusPts.size >= 2)
                                            listOf(stylusPts.first(), stylusPts.last()) else null,
                                        isFromStylus = isStylus
                                    )
                                )
                            }

                            ToolType.SHAPE -> if (moved && points.size >= 2) {
                                addStroke(
                                    DrawStroke(
                                        listOf(points.first(), points.last()),
                                        color,
                                        width = shapeWidthNow,
                                        shapeType = shapeType,
                                        isFilled = shapeFilledNow,
                                        stylusPoints = if (isStylus && stylusPts.size >= 2)
                                            listOf(stylusPts.first(), stylusPts.last()) else null,
                                        isFromStylus = isStylus
                                    )
                                )
                            }

                            else -> {}
                        }
                        clearStroke()
                    }

                    var mode = when (effectiveTool) {
                        ToolType.HAND -> GestureMode.PAN
                        ToolType.TEXT -> GestureMode.TEXT_PENDING
                        else -> GestureMode.DRAW
                    }

                    if (mode == GestureMode.DRAW) {
                        clearStroke()
                        activeTool = effectiveTool
                        strokeIsStylus = isStylus
                        addPoint(down, down.position)
                        if (effectiveTool == ToolType.ERASER) eraseAt(down.position)
                        down.consume()
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        event.changes.firstOrNull()?.let { lastTime = it.uptimeMillis }
                        if (pressed.size > maxPointers) maxPointers = pressed.size

                        // A second finger turns any finger gesture into pinch-zoom. A stylus
                        // stroke keeps going: the extra touch is most likely a resting palm.
                        if (pressed.size >= 2 && mode != GestureMode.TRANSFORM && mode != GestureMode.IGNORE &&
                            !(mode == GestureMode.DRAW && isStylus)
                        ) {
                            if (mode == GestureMode.DRAW) {
                                clearStroke()
                                hapticJob?.cancel()
                                if (drawingNotified) drawingChange(false)
                                drawingNotified = false
                            }
                            draggingTextId = null
                            textDragDelta = Offset.Zero
                            mode = GestureMode.TRANSFORM
                        }

                        if (pressed.isEmpty()) break

                        when (mode) {
                            GestureMode.TRANSFORM -> {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                if (centroid != Offset.Unspecified) {
                                    viewport.transform(centroid, pan, zoom)
                                }
                                transformTravel += pan.getDistance() +
                                    abs(1f - zoom) * event.calculateCentroidSize(useCurrent = true)
                                event.changes.forEach { it.consume() }
                            }

                            GestureMode.PAN -> {
                                val c = event.changes.firstOrNull { it.id == primaryId } ?: pressed.first()
                                viewport.panBy(c.positionChange())
                                c.consume()
                            }

                            GestureMode.TEXT_PENDING, GestureMode.TEXT_DRAG -> {
                                val c = event.changes.firstOrNull { it.id == primaryId } ?: pressed.first()
                                val delta = c.positionChange()
                                travel += delta.getDistance()
                                if (mode == GestureMode.TEXT_PENDING && travel > slop) {
                                    if (textHit != null) {
                                        mode = GestureMode.TEXT_DRAG
                                        draggingTextId = textHit.id
                                        textDragDelta = Offset.Zero
                                        HapticUtil.performToggleOn(haptics)
                                    } else {
                                        mode = GestureMode.PAN
                                    }
                                }
                                when (mode) {
                                    GestureMode.TEXT_DRAG -> textDragDelta += delta / viewport.scale
                                    GestureMode.PAN -> viewport.panBy(delta)
                                    else -> {}
                                }
                                c.consume()
                            }

                            GestureMode.DRAW -> {
                                val c = event.changes.firstOrNull { it.id == primaryId }
                                if (c == null || !c.pressed) {
                                    // The stylus lifted while a palm is still down: finish the stroke
                                    c?.let { addPoint(it, it.position) }
                                    commitStroke()
                                    hapticJob?.cancel()
                                    if (drawingNotified) drawingChange(false)
                                    drawingNotified = false
                                    mode = GestureMode.IGNORE
                                } else {
                                    c.historical.forEach { h -> addPoint(c, h.position) }
                                    addPoint(c, c.position)
                                    travel += c.positionChange().getDistance()
                                    if (!drawingNotified && travel > slop) {
                                        drawingNotified = true
                                        drawingChange(true)
                                    }
                                    if (activeTool == ToolType.ERASER) eraseAt(c.position)

                                    // Speed-dependent texture haptics while inking
                                    val dt = (c.uptimeMillis - lastSpeedTime).coerceAtLeast(1L)
                                    val dist = (c.position - lastSpeedPos).getDistance()
                                    speed = speed * 0.6f + (dist / dt * 1000f) * 0.4f
                                    lastSpeedTime = c.uptimeMillis
                                    lastSpeedPos = c.position
                                    if (activeTool == ToolType.PEN && hapticJob == null && drawingNotified) {
                                        hapticJob = scope.launch {
                                            while (isActive) {
                                                val sp = speed
                                                if (sp < 60f) {
                                                    delay(180L)
                                                    continue
                                                }
                                                val t = (440f * (1f - (sp.coerceAtMost(300f) / 300f))).toLong() + 10L
                                                HapticUtil.performLightTick(haptics)
                                                delay(max(20L, t))
                                            }
                                        }
                                    }
                                    c.consume()
                                }
                            }

                            GestureMode.IGNORE -> event.changes.forEach { it.consume() }
                        }
                    }

                    // Gesture finished: every pointer is up
                    hapticJob?.cancel()
                    when (mode) {
                        GestureMode.DRAW -> commitStroke()
                        GestureMode.TEXT_PENDING -> {
                            textTap(downWorld, textHit?.id)
                            HapticUtil.performClick(haptics)
                        }

                        GestureMode.TEXT_DRAG -> {
                            val id = draggingTextId
                            val item = textsNow.firstOrNull { it.id == id }
                            if (item != null && textDragDelta != Offset.Zero) {
                                moveText(item.id, item.x + textDragDelta.x, item.y + textDragDelta.y)
                            }
                            draggingTextId = null
                            textDragDelta = Offset.Zero
                        }

                        GestureMode.TRANSFORM -> {
                            val quick = lastTime - startTime < MULTI_FINGER_TAP_TIMEOUT_MS
                            if (fingerGestures && quick && transformTravel < slop * 2) {
                                when (maxPointers) {
                                    2 -> {
                                        undo()
                                        HapticUtil.performClick(haptics)
                                    }

                                    3 -> {
                                        redo()
                                        HapticUtil.performClick(haptics)
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                    if (drawingNotified) drawingChange(false)
                    eraserCursor = null
                }
            }
    ) {
        val scale = viewport.scale
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY

        BackgroundDrawer.drawBackgroundOnCompose(
            this,
            canvasBackground,
            size.width,
            size.height,
            scale,
            offsetX,
            offsetY
        )

        bgImage?.let { imageBitmap ->
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(
                    (imageBitmap.width * scale).toInt(),
                    (imageBitmap.height * scale).toInt()
                )
            )
        }

        fun screen(p: Offset) = Offset(p.x * scale + offsetX, p.y * scale + offsetY)

        strokes.forEach { stroke ->
            val screenPoints = stroke.points.map(::screen)
            when {
                stroke.isArrow && screenPoints.size >= 2 -> with(StrokeDrawer) {
                    drawArrow(screenPoints.first(), screenPoints.last(), stroke.color, stroke.width * scale)
                }

                stroke.shapeType != null && screenPoints.size >= 2 -> with(StrokeDrawer) {
                    drawShape(
                        screenPoints.first(),
                        screenPoints.last(),
                        stroke.shapeType,
                        stroke.color,
                        stroke.width * scale,
                        stroke.isFilled
                    )
                }

                stroke.isFromStylus && stroke.stylusPoints != null && stroke.stylusPoints.size >= 2 -> {
                    drawPressureSensitiveStroke(
                        stroke.stylusPoints.map { it.copy(offset = screen(it.offset)) },
                        stroke.color,
                        stroke.width * scale,
                        stroke.style
                    )
                }

                screenPoints.size >= 2 -> drawScribbleStroke(
                    screenPoints,
                    stroke.color,
                    stroke.width * scale,
                    stroke.style
                )
            }
        }

        texts.forEach { t ->
            val delta = if (t.id == draggingTextId) textDragDelta else Offset.Zero
            val pos = screen(Offset(t.x + delta.x, t.y + delta.y))
            drawStringWithFont(context, t.text, pos.x, pos.y, t.size * scale, t.color.toArgb())
        }

        // Live preview of the stroke being drawn
        when (activeTool) {
            ToolType.PEN -> if (currentStroke.size >= 2) {
                if (strokeIsStylus && currentStylusPoints.size >= 2) {
                    drawPressureSensitiveStroke(
                        currentStylusPoints.map { it.copy(offset = screen(it.offset)) },
                        currentColor,
                        penWidth * scale,
                        currentPenStyle
                    )
                } else {
                    drawScribbleStroke(
                        currentStroke.map(::screen),
                        currentColor,
                        penWidth * scale,
                        currentPenStyle
                    )
                }
            }

            ToolType.ARROW -> if (currentStroke.size >= 2) with(StrokeDrawer) {
                drawArrow(
                    screen(currentStroke.first()),
                    screen(currentStroke.last()),
                    currentColor,
                    arrowWidth * scale
                )
            }

            ToolType.SHAPE -> if (currentStroke.size >= 2) with(StrokeDrawer) {
                drawShape(
                    screen(currentStroke.first()),
                    screen(currentStroke.last()),
                    currentShapeType,
                    currentColor,
                    shapeWidth * scale,
                    shapeFilled
                )
            }

            else -> {}
        }

        // Eraser cursor: a neutral ring that is visible on any background
        eraserCursor?.let { center ->
            drawCircle(color = Color.Gray.copy(alpha = 0.18f), radius = eraserRadiusPx, center = center)
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = eraserRadiusPx,
                center = center,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = eraserRadiusPx,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }
    }
}
