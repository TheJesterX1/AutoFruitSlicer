package com.example.fruitslicer

import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Core bot loop.
 *
 * Every [SCAN_INTERVAL_MS] ms:
 *  1. Grab the latest screen bitmap from [ScreenCaptureService]
 *  2. Run [FruitDetector] to find fruit centers
 *  3. For each fruit, dispatch a short swipe through it via [SlicerAccessibilityService]
 *
 * Swipes are randomized in angle/length to look more natural.
 */
object BotController {

    private const val TAG = "BotController"
    private const val SCAN_INTERVAL_MS = 80L   // ~12 scans/sec
    private const val SWIPE_DURATION_MS = 60L  // fast slash
    private const val SWIPE_LENGTH = 180f       // px radius of slash

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start() {
        if (job?.isActive == true) return
        Log.d(TAG, "Bot started")
        job = scope.launch {
            while (isActive) {
                tick()
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
        val bitmap = ScreenCaptureService.latestBitmap ?: return
        // Work on a copy so the service can keep updating the reference
        val bmp = try { bitmap.copy(bitmap.config, false) } catch (e: Exception) { return }

        try {
            val targets = FruitDetector.detect(bmp)
            if (targets.isEmpty()) return

            for (target in targets) {
                // Random slash angle so we don't always swipe the same direction
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val halfLen = SWIPE_LENGTH / 2f
                val x1 = target.x - halfLen * cos(angle)
                val y1 = target.y - halfLen * sin(angle)
                val x2 = target.x + halfLen * cos(angle)
                val y2 = target.y + halfLen * sin(angle)

                SlicerAccessibilityService.swipe(x1, y1, x2, y2, SWIPE_DURATION_MS)
                Log.v(TAG, "Swiped fruit at (${target.x.toInt()}, ${target.y.toInt()})")
            }
        } finally {
            bmp.recycle()
        }
    }
}
