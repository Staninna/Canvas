package com.sameerasw.canvas.fork.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.CanvasViewModel
import com.sameerasw.canvas.CropActivity
import com.sameerasw.canvas.R
import com.sameerasw.canvas.fork.settings.ForkSettingsActivity
import com.sameerasw.canvas.CanvasModel
import com.sameerasw.canvas.fork.ForkSettings
import com.sameerasw.canvas.data.TextItem
import com.sameerasw.canvas.model.PenStyle
import com.sameerasw.canvas.model.ShapeType
import com.sameerasw.canvas.model.ToolType
import com.sameerasw.canvas.ui.components.NotesRolePanel
import com.sameerasw.canvas.fork.ui.chrome.CanvasTopBar
import com.sameerasw.canvas.fork.ui.chrome.ToolDock
import com.sameerasw.canvas.fork.ui.chrome.ToolOptionsPanel
import com.sameerasw.canvas.fork.ui.dialogs.ForkTextInputDialog
import com.sameerasw.canvas.fork.ui.dialogs.ForkTextOptionsDialog
import com.sameerasw.canvas.ui.components.sheets.AboutBottomSheet
import com.sameerasw.canvas.ui.drawing.BitmapExportHelper
import com.sameerasw.canvas.ui.drawing.BitmapStorageHelper
import com.sameerasw.canvas.ui.modifiers.BlurDirection
import com.sameerasw.canvas.ui.modifiers.progressiveBlur
import com.sameerasw.canvas.fork.ui.theme.isAppInDarkTheme
import com.sameerasw.canvas.util.GsonProvider
import com.sameerasw.canvas.utils.HapticUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CanvasApp(
    viewModel: CanvasViewModel,
    onPickBackground: () -> Unit,
    onRequestNotesRole: () -> Unit,
    onLaunchContentCapture: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val viewport = rememberCanvasViewportState()

    // Document
    val strokes by viewModel.strokes.collectAsState()
    val texts by viewModel.texts.collectAsState()
    val backgroundImageUri by viewModel.backgroundImageUri.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isBlurEnabled by viewModel.isBlurEnabled.collectAsState()

    // Notes role
    val stylusMode by viewModel.stylusMode.collectAsState()
    val launchedFromLockScreen by viewModel.launchedFromLockScreen.collectAsState()
    val isFloatingWindow by viewModel.isFloatingWindow.collectAsState()
    val isNotesRoleHeld by viewModel.isNotesRoleHeld.collectAsState()

    // Settings
    val canvasBackground by ForkSettings.canvasBackground.collectAsState()
    val autoHideUi by ForkSettings.autoHideUi.collectAsState()
    val fingerGestures by ForkSettings.fingerGestures.collectAsState()

    // Tools (survive rotation)
    val dark = isAppInDarkTheme()
    var currentTool by rememberSaveable { mutableStateOf(ToolType.PEN) }
    var colorArgb by rememberSaveable { mutableStateOf(if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()) }
    val currentColor = Color(colorArgb)
    var penStyle by rememberSaveable { mutableStateOf(PenStyle.NORMAL) }
    var penWidth by rememberSaveable { mutableFloatStateOf(4f) }
    var arrowWidth by rememberSaveable { mutableFloatStateOf(5f) }
    var shapeType by rememberSaveable { mutableStateOf(ShapeType.RECTANGLE) }
    var shapeWidth by rememberSaveable { mutableFloatStateOf(5f) }
    var shapeFilled by rememberSaveable { mutableStateOf(false) }
    var textSize by rememberSaveable { mutableFloatStateOf(48f) }
    var eraserSize by rememberSaveable { mutableFloatStateOf(24f) }

    // Default ink follows the theme, so it never disappears into the background
    LaunchedEffect(dark) {
        if (dark && colorArgb == 0xFF000000.toInt()) colorArgb = 0xFFFFFFFF.toInt()
        if (!dark && colorArgb == 0xFFFFFFFF.toInt()) colorArgb = 0xFF000000.toInt()
    }

    // UI state
    var optionsOpen by rememberSaveable { mutableStateOf(false) }
    var isDrawing by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showTextOptions by remember { mutableStateOf(false) }
    var pendingTextPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingTextValue by remember { mutableStateOf("") }
    var selectedTextId by remember { mutableStateOf<Long?>(null) }
    var canvasViewSize by remember { mutableStateOf(IntSize(1, 1)) }

    BackHandler(enabled = optionsOpen) { optionsOpen = false }

    fun message(text: String, undoable: Boolean = false) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = text,
                actionLabel = if (undoable) context.getString(R.string.action_undo) else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo()
        }
    }

    fun share() {
        if (strokes.isEmpty() && texts.isEmpty() && backgroundImageUri == null) {
            message(context.getString(R.string.msg_nothing_to_export))
            return
        }
        sharing = true
        scope.launch {
            try {
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val bmp = BitmapExportHelper.createBitmapFromData(
                    context,
                    strokes,
                    texts,
                    backgroundImageUri = backgroundImageUri,
                    outputWidth = canvasViewSize.width.coerceAtLeast(1),
                    outputHeight = canvasViewSize.height.coerceAtLeast(1)
                )
                val uri = bmp?.let {
                    BitmapStorageHelper.saveBitmapToCacheAndGetUri(
                        context, it, "canvas_full_$ts.png", Bitmap.CompressFormat.PNG
                    )
                }
                if (uri == null) {
                    message(context.getString(if (bmp == null) R.string.msg_nothing_to_export else R.string.msg_export_failed))
                } else {
                    val gson = GsonProvider.create()
                    context.startActivity(
                        Intent(context, CropActivity::class.java).apply {
                            putExtra("image_uri", uri.toString())
                            putExtra("background_image_uri", backgroundImageUri)
                            putExtra("is_share", true)
                            putExtra("strokes_json", gson.toJson(strokes))
                            putExtra("texts_json", gson.toJson(texts))
                        }
                    )
                }
            } finally {
                sharing = false
            }
        }
    }

    // Toolbars fade away while a stroke is being drawn
    val chromeAlpha by animateFloatAsState(
        targetValue = if (autoHideUi && isDrawing) 0f else 1f,
        animationSpec = tween(durationMillis = if (isDrawing) 140 else 320, delayMillis = if (isDrawing) 0 else 120),
        label = "chrome_alpha"
    )

    val statusBarHeightPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    val bottomBlurHeightPx = with(density) { 140.dp.toPx() }
    val eraserRadiusPx = with(density) { eraserSize.dp.toPx() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .progressiveBlur(
                    blurRadius = if (isBlurEnabled) 40f else 0f,
                    height = statusBarHeightPx * 1.5f,
                    direction = BlurDirection.TOP
                )
                .progressiveBlur(
                    blurRadius = if (isBlurEnabled) 20f else 0f,
                    height = bottomBlurHeightPx,
                    direction = BlurDirection.BOTTOM
                )
        ) {
            CanvasSurface(
                viewport = viewport,
                currentTool = currentTool,
                strokes = strokes,
                texts = texts,
                penWidth = penWidth,
                currentColor = currentColor,
                currentPenStyle = penStyle,
                currentShapeType = shapeType,
                arrowWidth = arrowWidth,
                shapeWidth = shapeWidth,
                shapeFilled = shapeFilled,
                eraserRadiusPx = eraserRadiusPx,
                canvasBackground = canvasBackground,
                fingerGesturesEnabled = fingerGestures,
                backgroundImageUri = backgroundImageUri,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasViewSize = it },
                onAddStroke = { viewModel.addStroke(it) },
                onErase = { erased, textIds ->
                    // One undo step for the whole eraser swipe, using upstream's public API only
                    val remaining = strokes.filterNot { it in erased }
                    if (textIds.isEmpty()) {
                        viewModel.setStrokes(remaining)
                    } else {
                        viewModel.importCanvasState(
                            CanvasModel(remaining, texts.filterNot { it.id in textIds }, backgroundImageUri)
                        )
                    }
                },
                onTextTap = { world, hitId ->
                    if (hitId != null) {
                        selectedTextId = hitId
                        showTextOptions = true
                    } else {
                        selectedTextId = null
                        pendingTextPosition = world
                        pendingTextValue = ""
                        showTextDialog = true
                    }
                },
                onMoveText = { id, x, y ->
                    texts.firstOrNull { it.id == id }?.let { viewModel.updateText(it.copy(x = x, y = y)) }
                },
                onUndo = {
                    if (canUndo) {
                        viewModel.undo()
                        message(context.getString(R.string.msg_undone))
                    }
                },
                onRedo = {
                    if (canRedo) {
                        viewModel.redo()
                        message(context.getString(R.string.msg_redone))
                    }
                },
                onDrawingChange = { isDrawing = it },
                onStylusButtonPressed = {
                    currentTool = if (currentTool == ToolType.ERASER) ToolType.PEN else ToolType.ERASER
                    HapticUtil.performToggleOn(haptics)
                }
            )
        }

        // While the options card is open, the first touch on the canvas just closes it
        if (optionsOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown().consume()
                            optionsOpen = false
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            )
        }

        CanvasTopBar(
            canUndo = canUndo,
            canRedo = canRedo,
            zoomPercent = viewport.zoomPercent,
            showZoom = viewport.isTransformed,
            sharing = sharing,
            hasBackgroundImage = backgroundImageUri != null,
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onResetZoom = { scope.launch { viewport.animateReset() } },
            onShare = { share() },
            onBackgroundImage = onPickBackground,
            onRemoveBackgroundImage = { viewModel.setBackgroundImageUri(null) },
            onInvert = {
                viewModel.invert()
                message(context.getString(R.string.msg_ink_inverted), undoable = true)
            },
            onClear = { showClearConfirm = true },
            onSettings = { context.startActivity(Intent(context, ForkSettingsActivity::class.java)) },
            onAbout = { showAbout = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = chromeAlpha }
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .graphicsLayer { alpha = chromeAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NotesRolePanel(
                visible = false,
                isNotesRoleHeld = isNotesRoleHeld,
                isFloatingWindow = isFloatingWindow,
                isLaunchedFromLockScreen = launchedFromLockScreen,
                stylusMode = stylusMode,
                onRequestNotesRole = onRequestNotesRole,
                onLaunchContentCapture = onLaunchContentCapture,
                modifier = Modifier.fillMaxWidth()
            )

            SnackbarHost(snackbar, Modifier.widthIn(max = 440.dp))

            AnimatedVisibility(
                visible = optionsOpen,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    scaleIn(
                        initialScale = 0.9f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                    ) +
                    slideInVertically(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) { it / 6 },
                exit = fadeOut(tween(120)) +
                    scaleOut(targetScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                    slideOutVertically(tween(160)) { it / 8 }
            ) {
                ToolOptionsPanel(
                    tool = currentTool,
                    color = currentColor,
                    penStyle = penStyle,
                    penWidth = penWidth,
                    arrowWidth = arrowWidth,
                    shapeType = shapeType,
                    shapeWidth = shapeWidth,
                    shapeFilled = shapeFilled,
                    textSize = textSize,
                    eraserSize = eraserSize,
                    onColorChange = { colorArgb = it.toArgbInt() },
                    onPenStyleChange = { penStyle = it },
                    onPenWidthChange = { penWidth = it },
                    onArrowWidthChange = { arrowWidth = it },
                    onShapeTypeChange = { shapeType = it },
                    onShapeWidthChange = { shapeWidth = it },
                    onShapeFilledChange = { shapeFilled = it },
                    onTextSizeChange = { textSize = it },
                    onEraserSizeChange = { eraserSize = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.size(10.dp))

            ToolDock(
                currentTool = currentTool,
                currentColor = currentColor,
                currentSizePx = when (currentTool) {
                    ToolType.ARROW -> arrowWidth
                    ToolType.SHAPE -> shapeWidth
                    ToolType.TEXT -> textSize / 4f
                    else -> penWidth
                },
                optionsOpen = optionsOpen,
                onToolClick = { tool ->
                    if (tool == currentTool) {
                        optionsOpen = !optionsOpen
                        HapticUtil.performClick(haptics)
                    } else {
                        currentTool = tool
                        HapticUtil.performToggleOn(haptics)
                    }
                },
                onSwatchClick = {
                    if (currentTool == ToolType.HAND || currentTool == ToolType.ERASER) {
                        currentTool = ToolType.PEN
                        optionsOpen = true
                    } else {
                        optionsOpen = !optionsOpen
                    }
                    HapticUtil.performClick(haptics)
                },
                modifier = Modifier.widthIn(max = 460.dp)
            )
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(painterResource(R.drawable.rounded_cleaning_services_24), contentDescription = null) },
            title = { Text(stringResource(R.string.clear_dialog_title)) },
            text = { Text(stringResource(R.string.clear_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        viewModel.save()
                        showClearConfirm = false
                        message(context.getString(R.string.msg_canvas_cleared), undoable = true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showAbout) {
        AboutBottomSheet(
            onDismissRequest = { showAbout = false },
            onToggleDeveloperMode = {}
        )
    }

    ForkTextOptionsDialog(
        visible = showTextOptions,
        text = texts.firstOrNull { it.id == selectedTextId }?.text.orEmpty(),
        onEdit = {
            texts.firstOrNull { it.id == selectedTextId }?.let { hit ->
                pendingTextValue = hit.text
                pendingTextPosition = Offset(hit.x, hit.y)
                showTextDialog = true
            }
            showTextOptions = false
        },
        onDelete = {
            selectedTextId?.let { viewModel.removeText(it) }
            selectedTextId = null
            showTextOptions = false
        },
        onCancel = {
            selectedTextId = null
            showTextOptions = false
        }
    )

    ForkTextInputDialog(
        visible = showTextDialog,
        isEditing = selectedTextId != null,
        currentValue = pendingTextValue,
        onValueChange = { pendingTextValue = it },
        onConfirm = {
            val editingId = selectedTextId
            val existing = texts.firstOrNull { it.id == editingId }
            if (editingId != null && existing != null) {
                viewModel.updateText(existing.copy(text = pendingTextValue))
            } else {
                viewModel.addText(
                    TextItem(
                        x = pendingTextPosition.x,
                        y = pendingTextPosition.y,
                        text = pendingTextValue,
                        size = textSize,
                        color = currentColor
                    )
                )
            }
            showTextDialog = false
            selectedTextId = null
        },
        onDismiss = {
            showTextDialog = false
            selectedTextId = null
        }
    )
}

private fun Color.toArgbInt(): Int = toArgb()
