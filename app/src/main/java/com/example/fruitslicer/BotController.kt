package com.example.fruitslicer

import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object BotController {

    private const val TAG = "BotController"
    private const val STARTUP_DELAY_MS = 5000L  // 5 seconds to switch to Fruit Ninja
    private const val SCAN_INTERVAL_MS = 120L
    private const val SWIPE_DURATION_MS = 50L
    private const val SWIPE_LENGTH = 200f

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Known Fruit Ninja package names
    private val FRUIT_NINJA_PACKAGES = setOf(
        "com.halfbrick.fruitninja",
        "com.halfbrick.fruitninjafree",
        "com.halfbrick.fruitninja2"
    )

    fun start() {
        if (job?.isActive == true) return
        Log.d(TAG, "Bot starting in ${STARTUP_DELAY_MS}ms...")
        job = scope.launch {
            // Wait so user can switch to Fruit Ninja
            delay(STARTUP_DELAY_MS)
            Log.d(TAG, "Bot running!")
            while (isActive) {
                try {
                    tick()
                } catch (e: Exception) {
                    Log.e(TAG, "Tick error: ${e.message}")
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Bot stopped")
        job?.cancel()
        job = null
    }

    private fun tick() {
        // Only run if Fruit Ninja is the foreground app
        val currentApp = SlicerAccessibilityService.currentPackage ?: return
        if (currentApp !in FRUIT_NINJA_PACKAGES) {
            Log.v(TAG, "Not Fruit Ninja (got $currentApp), skipping")
            return
        }

        val bitmap = ScreenCaptureService.latestBitmap ?: return
        val bmp = try {
            bitmap.copy(bitmap.config ?: return, false)
        } catch (e: Exception) { return }

        try {
            val targets = FruitDetector.detect(bmp)
            if (targets.isEmpty()) return

            for (target in targets) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val half = SWIPE_LENGTH / 2f
                SlicerAccessibilityService.swipe(
                    target.x - half * cos(angle),
                    target.y - half * sin(angle),
                    target.x + half * cos(angle),
                    target.y + half * sin(angle),
                    SWIPE_DURATION_MS
                )
            }
        } finally {
            bmp.recycle()
        }
    }
}
