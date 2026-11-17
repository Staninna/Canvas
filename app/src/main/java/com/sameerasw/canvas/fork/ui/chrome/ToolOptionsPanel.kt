package com.sameerasw.canvas.fork.ui.chrome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.canvas.R
import com.sameerasw.canvas.model.PenStyle
import com.sameerasw.canvas.model.ShapeType
import com.sameerasw.canvas.model.ToolType
import com.sameerasw.canvas.utils.HapticUtil

/** Curated ink palette: columns are hues, rows are core / light / deep. */
val InkPalette: List<List<Color>> = listOf(
    listOf(
        Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
        Color(0xFF43A047), Color(0xFF00ACC1), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFD81B60)
    ),
    listOf(
        Color(0xFF5F6368), Color(0xFFF1F3F4), Color(0xFFEF9A9A), Color(0xFFFFCC80), Color(0xFFFFF59D),
        Color(0xFFA5D6A7), Color(0xFF80DEEA), Color(0xFF90CAF9), Color(0xFFCE93D8), Color(0xFFF48FB1)
    ),
    listOf(
        Color(0xFF3C4043), Color(0xFFBDC1C6), Color(0xFFB71C1C), Color(0xFFE65100), Color(0xFFF9A825),
        Color(0xFF1B5E20), Color(0xFF006064), Color(0xFF0D47A1), Color(0xFF4A148C), Color(0xFF880E4F)
    ),
)

const val PEN_MIN = 1f
const val PEN_MAX = 48f
const val TEXT_MIN = 8f
const val TEXT_MAX = 128f
const val ERASER_MIN = 8f
const val ERASER_MAX = 80f

/**
 * Options for the current tool in one consistent card: style, size (with a true-to-scale
 * preview) and color.
 */
@Composable
fun ToolOptionsPanel(
    tool: ToolType,
    color: Color,
    penStyle: PenStyle,
    penWidth: Float,
    arrowWidth: Float,
    shapeType: ShapeType,
    shapeWidth: Float,
    shapeFilled: Boolean,
    textSize: Float,
    eraserSize: Float,
    onColorChange: (Color) -> Unit,
    onPenStyleChange: (PenStyle) -> Unit,
    onPenWidthChange: (Float) -> Unit,
    onArrowWidthChange: (Float) -> Unit,
    onShapeTypeChange: (ShapeType) -> Unit,
    onShapeWidthChange: (Float) -> Unit,
    onShapeFilledChange: (Boolean) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onEraserSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 440.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 10.dp,
        tonalElevation = 2.dp
    ) {
        AnimatedContent(
            targetState = tool,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessHigh)) using
                    SizeTransform(clip = false) { _, _ ->
                        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                    }
            },
            label = "tool_options"
        ) { t ->
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(toolIcon(t)),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        stringResource(toolLabel(t)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                when (t) {
                    ToolType.PEN -> {
                        PenStylePicker(penStyle, onPenStyleChange)
                        SizeRow(penWidth, PEN_MIN..PEN_MAX, onPenWidthChange) { StrokePreview(color, penWidth) }
                        PaletteGrid(color, onColorChange)
                    }

                    ToolType.ARROW -> {
                        SizeRow(arrowWidth, PEN_MIN..PEN_MAX, onArrowWidthChange) { StrokePreview(color, arrowWidth) }
                        PaletteGrid(color, onColorChange)
                    }

                    ToolType.SHAPE -> {
                        ShapePicker(shapeType, onShapeTypeChange)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(role = Role.Switch) { onShapeFilledChange(!shapeFilled) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.label_fill),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(checked = shapeFilled, onCheckedChange = onShapeFilledChange)
                        }
                        SizeRow(shapeWidth, PEN_MIN..PEN_MAX, onShapeWidthChange) { StrokePreview(color, shapeWidth) }
                        PaletteGrid(color, onColorChange)
                    }

                    ToolType.TEXT -> {
                        SizeRow(textSize, TEXT_MIN..TEXT_MAX, onTextSizeChange) {
                            Text(
                                "Aa",
                                color = color.readableOn(MaterialTheme.colorScheme.surfaceContainerHigh),
                                fontSize = (textSize / 3f).coerceIn(12f, 34f).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        PaletteGrid(color, onColorChange)
                        Hint(R.string.hint_text)
                    }

                    ToolType.ERASER -> {
                        SizeRow(eraserSize, ERASER_MIN..ERASER_MAX, onEraserSizeChange) {
                            val d by animateDpAsState(eraserSize.dp.coerceAtMost(44.dp) , label = "eraser")
                            Box(
                                Modifier
                                    .size(d)
                                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            )
                        }
                    }

                    ToolType.HAND -> Hint(R.string.hint_hand)
                }
            }
        }
    }
}

@Composable
private fun Hint(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Light ink on a light card (or dark on dark) would vanish in previews; nudge it visible. */
@Composable
private fun Color.readableOn(background: Color): Color {
    val diff = kotlin.math.abs(luminance() - background.luminance())
    return if (diff < 0.15f) MaterialTheme.colorScheme.onSurfaceVariant else this
}

@Composable
private fun StrokePreview(color: Color, widthPx: Float) {
    val density = LocalDensity.current
    val d by animateDpAsState(
        with(density) { widthPx.toDp() }.coerceIn(8.dp, 44.dp),
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
        label = "stroke_preview"
    )
    Box(
        Modifier
            .size(d)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

/** Label, value, slider and a live preview, with a soft haptic tick per step. */
@Composable
private fun SizeRow(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    preview: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val lastStep = remember { mutableIntStateOf(value.toInt()) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.label_size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.label_size_value, value.toInt()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value,
                onValueChange = { new ->
                    onChange(new)
                    val step = new.toInt()
                    if (step != lastStep.intValue) {
                        lastStep.intValue = step
                        val strength = (new - range.start) / (range.endInclusive - range.start)
                        HapticUtil.performVariableTick(haptics, strength)
                    }
                },
                valueRange = range,
                modifier = Modifier.weight(1f)
            )
            Box(Modifier.padding(start = 12.dp).size(48.dp), contentAlignment = Alignment.Center) {
                preview()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> SegmentedPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    icon: (T) -> Int,
    label: @Composable (T) -> String,
    showLabels: Boolean
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            val text = label(option)
            ToggleButton(
                checked = option == selected,
                onCheckedChange = {
                    HapticUtil.performClick(haptics)
                    onSelect(option)
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { if (!showLabels) contentDescription = text },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Icon(painterResource(icon(option)), contentDescription = null, modifier = Modifier.size(18.dp))
                if (showLabels) {
                    Spacer(Modifier.size(6.dp))
                    Text(text, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun PenStylePicker(selected: PenStyle, onSelect: (PenStyle) -> Unit) {
    SegmentedPicker(
        options = PenStyle.entries,
        selected = selected,
        onSelect = onSelect,
        icon = {
            when (it) {
                PenStyle.NORMAL -> R.drawable.rounded_stylus_fountain_pen_24
                PenStyle.SMOOTH -> R.drawable.stylus_pen_24px
                PenStyle.ROUGH -> R.drawable.stylus_pencil_24px
            }
        },
        label = {
            stringResource(
                when (it) {
                    PenStyle.NORMAL -> R.string.pen_style_ink
                    PenStyle.SMOOTH -> R.string.pen_style_smooth
                    PenStyle.ROUGH -> R.string.pen_style_pencil
                }
            )
        },
        showLabels = true
    )
}

@Composable
private fun ShapePicker(selected: ShapeType, onSelect: (ShapeType) -> Unit) {
    SegmentedPicker(
        options = ShapeType.entries,
        selected = selected,
        onSelect = onSelect,
        icon = {
            when (it) {
                ShapeType.RECTANGLE -> R.drawable.rounded_rectangle_24
                ShapeType.CIRCLE -> R.drawable.rounded_circle_24
                ShapeType.TRIANGLE -> R.drawable.rounded_triangle_24
                ShapeType.LINE -> R.drawable.rounded_line_24
            }
        },
        label = {
            stringResource(
                when (it) {
                    ShapeType.RECTANGLE -> R.string.shape_rectangle
                    ShapeType.CIRCLE -> R.string.shape_circle
                    ShapeType.TRIANGLE -> R.string.shape_triangle
                    ShapeType.LINE -> R.string.shape_line
                }
            )
        },
        showLabels = false
    )
}

@Composable
private fun PaletteGrid(selected: Color, onSelect: (Color) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.label_color),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        InkPalette.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { c ->
                    Swatch(
                        color = c,
                        selected = c == selected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            HapticUtil.performClick(haptics)
                            onSelect(c)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "swatch_ring"
    )
    val inset: Dp by animateDpAsState(
        if (selected) 4.dp else 0.dp,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "swatch_inset"
    )
    val hex = "#%06X".format(0xFFFFFF and color.toArgb())
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .border(2.dp, ring, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = hex
                this.selected = selected
            }
            .padding(inset),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    painterResource(R.drawable.rounded_check_24),
                    contentDescription = null,
                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
