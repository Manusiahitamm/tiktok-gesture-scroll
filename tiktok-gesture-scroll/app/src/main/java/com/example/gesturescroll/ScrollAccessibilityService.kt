package com.example.gesturescroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: ScrollAccessibilityService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    @Volatile var enabled = false
    private var lastSwipeAt = 0L

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { enabled = false }

    fun scrollDown() = swipe(upward = true)
    fun scrollUp() = swipe(upward = false)

    private fun swipe(upward: Boolean) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - lastSwipeAt < 850) return
        lastSwipeAt = now
        val dm = resources.displayMetrics
        val x = dm.widthPixels * 0.5f
        val startY = if (upward) dm.heightPixels * 0.78f else dm.heightPixels * 0.32f
        val endY = if (upward) dm.heightPixels * 0.30f else dm.heightPixels * 0.78f
        val path = Path().apply { moveTo(x, startY); lineTo(x, endY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        enabled = false
        super.onDestroy()
    }
}
