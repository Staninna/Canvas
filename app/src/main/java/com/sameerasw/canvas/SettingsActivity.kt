package com.sameerasw.canvas

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.canvas.ui.theme.CanvasTheme
import com.sameerasw.canvas.util.CanvasSharingHelper
import com.sameerasw.canvas.utils.HapticUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsRepository.init(this)
        enableEdgeToEdge()
        setContent {
            CanvasTheme {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val themeMode by SettingsRepository.themeMode.collectAsState()
    val canvasBackground by SettingsRepository.canvasBackground.collectAsState()
    val hapticsLevel by SettingsRepository.hapticsLevel.collectAsState()
    val useBlur by SettingsRepository.useBlur.collectAsState()
    val autoHideUi by SettingsRepository.autoHideUi.collectAsState()
    val fingerGestures by SettingsRepository.fingerGestures.collectAsState()

    val canvasViewModel: CanvasViewModel = viewModel()
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val model = CanvasModel(
                canvasViewModel.strokes.value,
                canvasViewModel.texts.value,
                canvasViewModel.backgroundImageUri.value
            )
            CanvasSharingHelper.exportCanvas(context, model, uri)
            HapticUtil.performSuccess(haptics)
            snackbar.showSnackbar(context.getString(R.string.msg_canvas_exported))
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            icon = { Icon(painterResource(R.drawable.rounded_download_24), contentDescription = null) },
            title = { Text(stringResource(R.string.import_warning_title)) },
            text = { Text(stringResource(R.string.import_warning_message)) },
            confirmButton = {
                Button(onClick = {
                    pendingImportUri = null
                    scope.launch {
                        val model = CanvasSharingHelper.importCanvas(context, uri)
                        if (model != null) {
                            canvasViewModel.importCanvasState(model)
                            HapticUtil.performSuccess(haptics)
                            snackbar.showSnackbar(context.getString(R.string.msg_canvas_imported))
                        } else {
                            snackbar.showSnackbar(context.getString(R.string.msg_import_failed))
                        }
                    }
                }) { Text(stringResource(R.string.action_import)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            painterResource(R.drawable.rounded_arrow_back_ios_new_24),
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                SectionHeader(R.string.settings_section_appearance)
                SettingsGroup {
                    ChoiceSetting(
                        title = R.string.settings_theme,
                        options = SettingsRepository.ThemeMode.entries,
                        selected = themeMode,
                        label = {
                            when (it) {
                                SettingsRepository.ThemeMode.LIGHT -> R.string.settings_theme_light
                                SettingsRepository.ThemeMode.DYNAMIC -> R.string.settings_theme_system
                                SettingsRepository.ThemeMode.DARK -> R.string.settings_theme_dark
                            }
                        },
                        icon = {
                            when (it) {
                                SettingsRepository.ThemeMode.LIGHT -> R.drawable.rounded_light_mode_24
                                SettingsRepository.ThemeMode.DYNAMIC -> R.drawable.rounded_devices_24
                                SettingsRepository.ThemeMode.DARK -> R.drawable.rounded_mode_night_24
                            }
                        },
                        onSelect = {
                            HapticUtil.performClick(haptics)
                            SettingsRepository.setThemeMode(it)
                        }
                    )
                    GroupDivider()
                    SwitchSetting(
                        title = R.string.settings_blur,
                        description = R.string.settings_blur_desc,
                        checked = useBlur,
                        onCheckedChange = { SettingsRepository.setUseBlur(it) }
                    )
                }

                SectionHeader(R.string.settings_section_canvas)
                SettingsGroup {
                    ChoiceSetting(
                        title = R.string.settings_background,
                        options = SettingsRepository.CanvasBackgroundType.entries,
                        selected = canvasBackground,
                        label = {
                            when (it) {
                                SettingsRepository.CanvasBackgroundType.NONE -> R.string.settings_background_none
                                SettingsRepository.CanvasBackgroundType.DOTS -> R.string.settings_background_dots
                                SettingsRepository.CanvasBackgroundType.LINES -> R.string.settings_background_lines
                            }
                        },
                        onSelect = {
                            HapticUtil.performClick(haptics)
                            SettingsRepository.setCanvasBackground(it)
                        }
                    )
                    GroupDivider()
                    SwitchSetting(
                        title = R.string.settings_auto_hide,
                        description = R.string.settings_auto_hide_desc,
                        checked = autoHideUi,
                        onCheckedChange = { SettingsRepository.setAutoHideUi(it) }
                    )
                    GroupDivider()
                    SwitchSetting(
                        title = R.string.settings_finger_gestures,
                        description = R.string.settings_finger_gestures_desc,
                        checked = fingerGestures,
                        onCheckedChange = { SettingsRepository.setFingerGestures(it) }
                    )
                }

                SectionHeader(R.string.settings_section_feel)
                SettingsGroup {
                    ChoiceSetting(
                        title = R.string.settings_haptics,
                        options = SettingsRepository.HapticsLevel.entries,
                        selected = hapticsLevel,
                        label = {
                            when (it) {
                                SettingsRepository.HapticsLevel.OFF -> R.string.settings_haptics_off
                                SettingsRepository.HapticsLevel.MIN -> R.string.settings_haptics_min
                                SettingsRepository.HapticsLevel.FULL -> R.string.settings_haptics_full
                            }
                        },
                        onSelect = {
                            SettingsRepository.setHapticsLevel(it)
                            HapticUtil.performToggleOn(haptics)
                        }
                    )
                }

                SectionHeader(R.string.settings_section_data)
                SettingsGroup {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            stringResource(R.string.label_share_canvas),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_share_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("canvas-$ts.canvas")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(painterResource(R.drawable.rounded_ios_share_24), null, Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.action_export))
                            }
                            FilledTonalButton(
                                onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(painterResource(R.drawable.rounded_download_24), null, Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.action_import))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun GroupDivider() {
    androidx.compose.material3.HorizontalDivider(
        Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SwitchSetting(
    title: Int,
    description: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) {
                HapticUtil.performClick(haptics)
                onCheckedChange(!checked)
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(16.dp))
        Switch(checked = checked, onCheckedChange = {
            HapticUtil.performClick(haptics)
            onCheckedChange(it)
        })
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ChoiceSetting(
    title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    icon: ((T) -> Int)? = null,
    onSelect: (T) -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            options.forEachIndexed { index, option ->
                ToggleButton(
                    checked = option == selected,
                    onCheckedChange = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    if (icon != null) {
                        Icon(painterResource(icon(option)), contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(stringResource(label(option)), maxLines = 1)
                }
            }
        }
    }
}
