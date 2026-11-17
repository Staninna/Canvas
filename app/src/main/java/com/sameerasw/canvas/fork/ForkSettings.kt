package com.sameerasw.canvas.fork

import android.content.Context
import android.content.SharedPreferences
import com.sameerasw.canvas.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fork-only settings layer.
 *
 * Upstream settings stay owned by [SettingsRepository] (unchanged, so syncs merge cleanly): this
 * object only watches upstream's preferences file and exposes each value as a [StateFlow], so the
 * fork UI updates instantly instead of polling. Settings that exist only in the fork live in their
 * own preferences file.
 */
object ForkSettings {
    // Must match SettingsRepository's file and keys
    private const val UPSTREAM_PREFS = "canvas_prefs"
    private const val KEY_HAPTICS_LEVEL = "haptics_level"
    private const val KEY_CANVAS_BACKGROUND = "canvas_background"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_USE_BLUR = "use_blur"

    private const val FORK_PREFS = "canvas_fork_prefs"
    private const val KEY_AUTO_HIDE_UI = "auto_hide_ui"
    private const val KEY_FINGER_GESTURES = "finger_gestures"

    private var upstream: SharedPreferences? = null
    private var fork: SharedPreferences? = null

    private val _hapticsLevel = MutableStateFlow(SettingsRepository.HapticsLevel.FULL)
    val hapticsLevel: StateFlow<SettingsRepository.HapticsLevel> = _hapticsLevel.asStateFlow()

    private val _canvasBackground = MutableStateFlow(SettingsRepository.CanvasBackgroundType.NONE)
    val canvasBackground: StateFlow<SettingsRepository.CanvasBackgroundType> = _canvasBackground.asStateFlow()

    private val _themeMode = MutableStateFlow(SettingsRepository.ThemeMode.DYNAMIC)
    val themeMode: StateFlow<SettingsRepository.ThemeMode> = _themeMode.asStateFlow()

    private val _useBlur = MutableStateFlow(true)
    val useBlur: StateFlow<Boolean> = _useBlur.asStateFlow()

    private val _autoHideUi = MutableStateFlow(true)
    /** Fade the toolbars out while a stroke is being drawn (ON by default). */
    val autoHideUi: StateFlow<Boolean> = _autoHideUi.asStateFlow()

    private val _fingerGestures = MutableStateFlow(true)
    /** Two-finger tap to undo, three-finger tap to redo (ON by default). */
    val fingerGestures: StateFlow<Boolean> = _fingerGestures.asStateFlow()

    // Kept as a field: SharedPreferences only holds listeners weakly.
    private val upstreamListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> readUpstream() }

    fun init(context: Context) {
        SettingsRepository.init(context)
        if (upstream != null) return
        val app = context.applicationContext
        upstream = app.getSharedPreferences(UPSTREAM_PREFS, Context.MODE_PRIVATE).also {
            it.registerOnSharedPreferenceChangeListener(upstreamListener)
        }
        fork = app.getSharedPreferences(FORK_PREFS, Context.MODE_PRIVATE).also {
            _autoHideUi.value = it.getBoolean(KEY_AUTO_HIDE_UI, true)
            _fingerGestures.value = it.getBoolean(KEY_FINGER_GESTURES, true)
        }
        readUpstream()
    }

    private fun readUpstream() {
        val p = upstream ?: return
        _hapticsLevel.value = SettingsRepository.HapticsLevel.fromValue(
            p.getInt(KEY_HAPTICS_LEVEL, SettingsRepository.HapticsLevel.FULL.value)
        )
        _canvasBackground.value = SettingsRepository.CanvasBackgroundType.fromValue(
            p.getInt(KEY_CANVAS_BACKGROUND, SettingsRepository.CanvasBackgroundType.NONE.value)
        )
        _themeMode.value = SettingsRepository.ThemeMode.fromValue(
            p.getInt(KEY_THEME_MODE, SettingsRepository.ThemeMode.DYNAMIC.value)
        )
        _useBlur.value = p.getBoolean(KEY_USE_BLUR, true)
    }

    // Upstream settings are written through SettingsRepository; the listener updates the flows.
    fun setHapticsLevel(level: SettingsRepository.HapticsLevel) = SettingsRepository.setHapticsLevel(level)
    fun setCanvasBackground(type: SettingsRepository.CanvasBackgroundType) = SettingsRepository.setCanvasBackground(type)
    fun setThemeMode(mode: SettingsRepository.ThemeMode) = SettingsRepository.setThemeMode(mode)
    fun setUseBlur(enabled: Boolean) = SettingsRepository.setUseBlur(enabled)

    fun setAutoHideUi(enabled: Boolean) {
        _autoHideUi.value = enabled
        fork?.edit()?.putBoolean(KEY_AUTO_HIDE_UI, enabled)?.apply()
    }

    fun setFingerGestures(enabled: Boolean) {
        _fingerGestures.value = enabled
        fork?.edit()?.putBoolean(KEY_FINGER_GESTURES, enabled)?.apply()
    }
}
