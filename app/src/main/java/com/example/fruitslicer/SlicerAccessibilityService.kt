package com.example.fruitslicer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class SlicerAccessibilityService : AccessibilityService() {

    companion object {
        var instance: SlicerAccessibilityService? = null
            private set

        // Tracks which app is currently in the foreground
        @Volatile var currentPackage: String? = null

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track which app is in the foreground
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrEmpty()) {
                currentPackage = pkg
            }
        }
    }

    override fun onInterrupt() {}
}
