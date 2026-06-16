package com.example.fruitslicer

import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object BotController {

    private const val TAG = "BotController"
    private const val SCAN_INTERVAL_MS = 100L
    private const val SWIPE_DURATION_MS = 50L
    private const val SWIPE_LENGTH = 200f

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
        val bmp = try { bitmap.copy(bitmap.config ?: return, false) } catch (e: Exception) { return }

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
                Log.v(TAG, "Sliced at (${target.x.toInt()}, ${target.y.toInt()})")
            }
        } finally {
            bmp.recycle()
        }
    }
}
