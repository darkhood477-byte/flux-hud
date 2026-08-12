package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("flux_settings", Context.MODE_PRIVATE)

    private val _ringOpacity = MutableStateFlow(prefs.getFloat("ring_opacity", 0.85f))
    val ringOpacity: StateFlow<Float> = _ringOpacity.asStateFlow()

    private val _labelStyle = MutableStateFlow(prefs.getString("label_style", "ICONS_AND_WORDS") ?: "ICONS_AND_WORDS")
    val labelStyle: StateFlow<String> = _labelStyle.asStateFlow()
    
    private val _isImmersiveModeActive = MutableStateFlow(prefs.getBoolean("immersive_mode", false))
    val isImmersiveModeActive: StateFlow<Boolean> = _isImmersiveModeActive.asStateFlow()

    fun setRingOpacity(opacity: Float) {
        prefs.edit().putFloat("ring_opacity", opacity).apply()
        _ringOpacity.value = opacity
    }

    fun setLabelStyle(style: String) {
        prefs.edit().putString("label_style", style).apply()
        _labelStyle.value = style
    }

    private val defaultHudItems = "folders,floating,caffeine,timer,clipboard,qr,apps,settings,p2_1,p2_2,p2_3,p2_4,p2_5,p2_6,p2_7,p2_8"
    private val _hudItems = MutableStateFlow(prefs.getString("hud_items", defaultHudItems)!!.split(","))
    val hudItems: StateFlow<List<String>> = _hudItems.asStateFlow()

    private val _accentColor = MutableStateFlow(prefs.getString("accent_color", "DEFAULT") ?: "DEFAULT")
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()

    fun setAccentColor(colorHex: String) {
        prefs.edit().putString("accent_color", colorHex).apply()
        _accentColor.value = colorHex
    }

    fun setHudItems(items: List<String>) {
        val joined = items.joinToString(",")
        prefs.edit().putString("hud_items", joined).apply()
        _hudItems.value = items
    }

    
    fun setImmersiveMode(active: Boolean) {
        prefs.edit().putBoolean("immersive_mode", active).apply()
        _isImmersiveModeActive.value = active
    }
}
