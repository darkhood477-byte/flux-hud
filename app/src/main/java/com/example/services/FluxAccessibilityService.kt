package com.example.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class FluxAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private var instance: FluxAccessibilityService? = null

        fun takeScreenshot(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        }
    }
}
