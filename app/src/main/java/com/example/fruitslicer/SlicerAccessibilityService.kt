package com.example.fruitslicer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

/**
 * Singleton-accessible accessibility service.
 * BotController calls [swipe] to inject gestures into whatever app is on screen.
 */
class SlicerAccessibilityService : AccessibilityService() {

    companion object {
        var instance: SlicerAccessibilityService? = null
            private set

        /**
         * Dispatch a swipe from (x1,y1) to (x2,y2) over [durationMs] milliseconds.
         */
        fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 80) {
            val svc = instance ?: return
            val path = Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            svc.dispatchGesture(gesture, null, null)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // We don't need to handle accessibility events — only gesture dispatch
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
