package com.sameerasw.canvas

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferences-backed settings. Every setting is also exposed as a [StateFlow] so the UI
 * reacts instantly when it changes (no polling).
 */
object SettingsRepository {
    private const val PREFS_NAME = "canvas_prefs"
    private const val KEY_HAPTICS_LEVEL = "haptics_level"
    private const val KEY_CANVAS_BACKGROUND = "canvas_background"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_USE_BLUR = "use_blur"
    private const val KEY_AUTO_HIDE_UI = "auto_hide_ui"
    private const val KEY_FINGER_GESTURES = "finger_gestures"

    enum class HapticsLevel(val value: Int) {
        OFF(0),
        MIN(1),
        FULL(2);

        companion object {
            fun fromValue(v: Int) = HapticsLevel.entries.firstOrNull { it.value == v } ?: FULL
        }
    }

    enum class CanvasBackgroundType(val value: Int) {
        NONE(0),
        DOTS(1),
        LINES(2);

        companion object {
            fun fromValue(v: Int) = CanvasBackgroundType.entries.firstOrNull { it.value == v } ?: NONE
        }
    }

    enum class ThemeMode(val value: Int) {
        LIGHT(0),
        DYNAMIC(1),
        DARK(2);

        companion object {
            fun fromValue(v: Int) = ThemeMode.entries.firstOrNull { it.value == v } ?: DYNAMIC
        }
    }

    private var prefs: SharedPreferences? = null

    private val _hapticsLevel = MutableStateFlow(HapticsLevel.FULL)
    val hapticsLevel: StateFlow<HapticsLevel> = _hapticsLevel.asStateFlow()

    private val _canvasBackground = MutableStateFlow(CanvasBackgroundType.NONE)
    val canvasBackground: StateFlow<CanvasBackgroundType> = _canvasBackground.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DYNAMIC)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _useBlur = MutableStateFlow(true)
    val useBlur: StateFlow<Boolean> = _useBlur.asStateFlow()

    private val _autoHideUi = MutableStateFlow(true)
    val autoHideUi: StateFlow<Boolean> = _autoHideUi.asStateFlow()

    private val _fingerGestures = MutableStateFlow(true)
    val fingerGestures: StateFlow<Boolean> = _fingerGestures.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            _hapticsLevel.value = HapticsLevel.fromValue(p.getInt(KEY_HAPTICS_LEVEL, HapticsLevel.FULL.value))
            _canvasBackground.value = CanvasBackgroundType.fromValue(
                p.getInt(KEY_CANVAS_BACKGROUND, CanvasBackgroundType.NONE.value)
            )
            _themeMode.value = ThemeMode.fromValue(p.getInt(KEY_THEME_MODE, ThemeMode.DYNAMIC.value))
            _useBlur.value = p.getBoolean(KEY_USE_BLUR, true)
            _autoHideUi.value = p.getBoolean(KEY_AUTO_HIDE_UI, true)
            _fingerGestures.value = p.getBoolean(KEY_FINGER_GESTURES, true)
        }
    }

    fun getHapticsLevel(): HapticsLevel = _hapticsLevel.value

    fun setHapticsLevel(level: HapticsLevel) {
        _hapticsLevel.value = level
        prefs?.edit()?.putInt(KEY_HAPTICS_LEVEL, level.value)?.apply()
    }

    fun getCanvasBackground(): CanvasBackgroundType = _canvasBackground.value

    fun setCanvasBackground(background: CanvasBackgroundType) {
        _canvasBackground.value = background
        prefs?.edit()?.putInt(KEY_CANVAS_BACKGROUND, background.value)?.apply()
    }

    fun getThemeMode(): ThemeMode = _themeMode.value

    fun setThemeMode(theme: ThemeMode) {
        _themeMode.value = theme
        prefs?.edit()?.putInt(KEY_THEME_MODE, theme.value)?.apply()
    }

    fun getUseBlur(): Boolean = _useBlur.value

    fun setUseBlur(enabled: Boolean) {
        _useBlur.value = enabled
        prefs?.edit()?.putBoolean(KEY_USE_BLUR, enabled)?.apply()
    }

    /** Fade the toolbars out while a stroke is being drawn (ON by default). */
    fun setAutoHideUi(enabled: Boolean) {
        _autoHideUi.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_HIDE_UI, enabled)?.apply()
    }

    /** Two-finger tap to undo, three-finger tap to redo (ON by default). */
    fun setFingerGestures(enabled: Boolean) {
        _fingerGestures.value = enabled
        prefs?.edit()?.putBoolean(KEY_FINGER_GESTURES, enabled)?.apply()
    }
}
