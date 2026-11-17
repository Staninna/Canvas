package com.sameerasw.canvas.fork

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.canvas.CanvasViewModel
import com.sameerasw.canvas.fork.ui.CanvasApp
import com.sameerasw.canvas.fork.ui.theme.ForkTheme

/** Entry point of the fork UI, called from MainActivity.setContent (the only hook into upstream). */
@Composable
fun ForkCanvasRoot(
    viewModel: CanvasViewModel,
    onPickBackground: () -> Unit,
    onRequestNotesRole: () -> Unit,
    onLaunchContentCapture: () -> Unit
) {
    val context = LocalContext.current
    remember { ForkSettings.init(context) }
    ForkTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CanvasApp(
                viewModel = viewModel,
                onPickBackground = onPickBackground,
                onRequestNotesRole = onRequestNotesRole,
                onLaunchContentCapture = onLaunchContentCapture
            )
        }
    }
}
