package com.sameerasw.canvas.fork.ui.chrome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.R
import com.sameerasw.canvas.model.ToolType

/** Tools in dock order. */
val DockTools = listOf(
    ToolType.HAND,
    ToolType.PEN,
    ToolType.ARROW,
    ToolType.SHAPE,
    ToolType.TEXT,
    ToolType.ERASER,
)

@DrawableRes
fun toolIcon(tool: ToolType): Int = when (tool) {
    ToolType.HAND -> R.drawable.rounded_back_hand_24
    ToolType.PEN -> R.drawable.rounded_stylus_fountain_pen_24
    ToolType.ARROW -> R.drawable.round_arrow_upward_24
    ToolType.SHAPE -> R.drawable.shape_line_24px
    ToolType.TEXT -> R.drawable.rounded_text_fields_24
    ToolType.ERASER -> R.drawable.rounded_ink_eraser_24
}

@StringRes
fun toolLabel(tool: ToolType): Int = when (tool) {
    ToolType.HAND -> R.string.tool_hand
    ToolType.PEN -> R.string.tool_pen
    ToolType.ARROW -> R.string.tool_arrow
    ToolType.SHAPE -> R.string.tool_shape
    ToolType.TEXT -> R.string.tool_text
    ToolType.ERASER -> R.string.tool_eraser
}

private val dockSpring = spring<Dp>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * The floating tool dock: every tool one tap away, a selection pill that glides between them,
 * and a live swatch showing the current color and size. Tapping the selected tool or the swatch
 * opens the tool's options.
 */
@Composable
fun ToolDock(
    currentTool: ToolType,
    currentColor: Color,
    currentSizePx: Float,
    optionsOpen: Boolean,
    onToolClick: (ToolType) -> Unit,
    onSwatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        BoxWithConstraints(Modifier.padding(6.dp)) {
            // Fit 6 tools + swatch + divider on narrow phones, cap the size on big screens.
            val cell = ((maxWidth - 17.dp) / (DockTools.size + 1)).coerceIn(40.dp, 52.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    val selectedIndex = DockTools.indexOf(currentTool).coerceAtLeast(0)
                    val indicatorX by animateDpAsState(cell * selectedIndex, dockSpring, label = "indicator_x")
                    val corner by animateDpAsState(
                        if (optionsOpen) 14.dp else cell / 2,
                        dockSpring,
                        label = "indicator_corner"
                    )
                    Box(
                        Modifier
                            .offset(x = indicatorX)
                            .size(cell)
                            .clip(RoundedCornerShape(corner))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Row {
                        DockTools.forEach { tool ->
                            DockToolButton(
                                tool = tool,
                                selected = tool == currentTool,
                                size = cell,
                                onClick = { onToolClick(tool) }
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .height(cell * 0.5f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                ColorSizeSwatch(
                    color = currentColor,
                    sizePx = currentSizePx,
                    cell = cell,
                    onClick = onSwatchClick
                )
            }
        }
    }
}

@Composable
private fun DockToolButton(
    tool: ToolType,
    selected: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tool_tint"
    )
    val iconScale by animateFloatAsState(
        if (selected) 1.08f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tool_scale"
    )
    val label = stringResource(toolLabel(tool))
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(toolIcon(tool)),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}

/** A ring in the current color with a dot whose size follows the stroke width. */
@Composable
private fun ColorSizeSwatch(
    color: Color,
    sizePx: Float,
    cell: Dp,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val animatedColor by animateColorAsState(color, label = "swatch_color")
    val dot by animateDpAsState(
        with(density) { sizePx.toDp() }.coerceIn(4.dp, cell * 0.5f),
        label = "swatch_dot"
    )
    val ringColor = MaterialTheme.colorScheme.outline
    val desc = stringResource(R.string.cd_color_and_size)
    val onColorIsLight = color.luminance() > 0.5f
    Box(
        modifier = Modifier
            .size(cell)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = desc
                stateDescription = "${sizePx.toInt()} px"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(cell * 0.72f)
                .clip(CircleShape)
                .background(animatedColor)
                .border(1.dp, ringColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(dot)
                    .clip(CircleShape)
                    .background(if (onColorIsLight) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.8f))
            )
        }
    }
}
