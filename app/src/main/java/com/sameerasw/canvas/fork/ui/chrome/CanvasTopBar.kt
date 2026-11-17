package com.sameerasw.canvas.fork.ui.chrome

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.R
import com.sameerasw.canvas.utils.HapticUtil

/** Floating top controls. Each cluster is its own pill so the canvas shows between them. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CanvasTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    zoomPercent: Int,
    showZoom: Boolean,
    sharing: Boolean,
    hasBackgroundImage: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetZoom: () -> Unit,
    onShare: () -> Unit,
    onBackgroundImage: () -> Unit,
    onRemoveBackgroundImage: () -> Unit,
    onInvert: () -> Unit,
    onClear: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Undo / redo
        BarPill(Modifier.align(Alignment.CenterStart)) {
            BarIconButton(
                icon = R.drawable.rounded_undo_24,
                label = stringResource(R.string.action_undo),
                enabled = canUndo,
                onClick = { HapticUtil.performClick(haptics); onUndo() }
            )
            BarIconButton(
                icon = R.drawable.rounded_redo_24,
                label = stringResource(R.string.action_redo),
                enabled = canRedo,
                onClick = { HapticUtil.performClick(haptics); onRedo() }
            )
        }

        // Zoom level, only when the view is not at home; tap to spring back
        AnimatedVisibility(
            visible = showZoom,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn() + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            ),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            val desc = stringResource(R.string.cd_reset_view, zoomPercent)
            Surface(
                onClick = { HapticUtil.performClick(haptics); onResetZoom() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shadowElevation = 6.dp,
                modifier = Modifier.semantics { contentDescription = desc }
            ) {
                Text(
                    text = stringResource(R.string.zoom_percent, zoomPercent),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Share + overflow
        BarPill(Modifier.align(Alignment.CenterEnd)) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (sharing) {
                    LoadingIndicator(Modifier.size(32.dp))
                } else {
                    BarIconButton(
                        icon = R.drawable.rounded_ios_share_24,
                        label = stringResource(R.string.action_share),
                        onClick = { HapticUtil.performClick(haptics); onShare() }
                    )
                }
            }
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                BarIconButton(
                    icon = R.drawable.rounded_more_vert_24,
                    label = stringResource(R.string.action_more),
                    onClick = { HapticUtil.performClick(haptics); menuOpen = true }
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    MenuItem(R.drawable.rounded_image_24, R.string.action_background_image) {
                        menuOpen = false; onBackgroundImage()
                    }
                    if (hasBackgroundImage) {
                        MenuItem(R.drawable.rounded_delete_24, R.string.action_remove_background_image) {
                            menuOpen = false; onRemoveBackgroundImage()
                        }
                    }
                    MenuItem(R.drawable.rounded_invert_colors_24, R.string.action_invert) {
                        menuOpen = false; onInvert()
                    }
                    MenuItem(
                        R.drawable.rounded_cleaning_services_24,
                        R.string.action_clear_canvas,
                        color = MaterialTheme.colorScheme.error
                    ) {
                        menuOpen = false; onClear()
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    MenuItem(R.drawable.rounded_settings_24, R.string.action_settings) {
                        menuOpen = false; onSettings()
                    }
                    MenuItem(R.drawable.rounded_info_24, R.string.action_about) {
                        menuOpen = false; onAbout()
                    }
                }
            }
        }
    }
}

@Composable
private fun BarPill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

@Composable
private fun BarIconButton(
    @DrawableRes icon: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    ) {
        Icon(painterResource(icon), contentDescription = label)
    }
}

@Composable
private fun MenuItem(
    @DrawableRes icon: Int,
    label: Int,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color
    DropdownMenuItem(
        text = {
            Text(
                stringResource(label),
                style = MaterialTheme.typography.bodyLarge,
                color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
            )
        },
        leadingIcon = { Icon(painterResource(icon), contentDescription = null, tint = tint) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
