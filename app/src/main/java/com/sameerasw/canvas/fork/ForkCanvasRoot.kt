package com.sameerasw.canvas.fork

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.canvas.CanvasViewModel
import com.sameerasw.canvas.fork.ui.CanvasApp
import com.sameerasw.canvas.fork.ui.theme.ForkTheme
import kotlinx.coroutines.launch

/** Entry point of the fork UI, called from MainActivity.setContent (the only hook into upstream). */
@Composable
fun ForkCanvasRoot(
    viewModel: CanvasViewModel,
    onRequestNotesRole: () -> Unit,
    onLaunchContentCapture: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    remember { ForkSettings.init(context) }

    // Picked backgrounds are copied into app storage so backups (Seedvault) carry them;
    // a photo picker grant does not survive a restore.
    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            BackgroundStore.copyIn(context, uri)?.let { viewModel.setBackgroundImageUri(it); viewModel.save() }
        }
    }

    // Moves backgrounds saved before that (content://) or restored into another profile.
    val backgroundImageUri by viewModel.backgroundImageUri.collectAsState()
    LaunchedEffect(backgroundImageUri) {
        BackgroundStore.cleanUpOnce(context)
        val stored = backgroundImageUri ?: return@LaunchedEffect
        BackgroundStore.migrate(context, stored)?.let {
            viewModel.setBackgroundImageUri(it)
            viewModel.save()
        }
    }

    ForkTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CanvasApp(
                viewModel = viewModel,
                onPickBackground = {
                    pickBackground.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRequestNotesRole = onRequestNotesRole,
                onLaunchContentCapture = onLaunchContentCapture
            )
        }
    }
}
