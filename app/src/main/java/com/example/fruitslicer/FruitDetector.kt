package com.example.fruitslicer

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Scans a bitmap for fruit-like colors and returns a list of (x, y) center points.
 *
 * Fruit Ninja fruits are typically bright reds, oranges, yellows, greens, and purples.
 * We sample a grid across the screen and flag pixels whose HSV hue/saturation match
 * known fruit palettes. Neighboring hits are clustered into single target points.
 */
object FruitDetector {

    // How densely we sample the screen (every N pixels)
    private const val SAMPLE_STEP = 12

    // Minimum cluster size to be considered a "real" fruit (filters noise)
    private const val MIN_CLUSTER_SIZE = 6

    // Cluster merge radius in pixels
    private const val CLUSTER_RADIUS = 80

    data class FruitTarget(val x: Float, val y: Float)

    /**
     * Returns a list of detected fruit center points from the given bitmap.
     */
    fun detect(bitmap: Bitmap): List<FruitTarget> {
        val width = bitmap.width
        val height = bitmap.height

        // Skip top 15% (score bar) and bottom 5% (UI) to avoid false positives
        val startY = (height * 0.15).toInt()
        val endY = (height * 0.95).toInt()

        val hits = mutableListOf<Pair<Int, Int>>()

        val hsv = FloatArray(3)
        for (y in startY until endY step SAMPLE_STEP) {
            for (x in 0 until width step SAMPLE_STEP) {
                val pixel = bitmap.getPixel(x, y)
                Color.colorToHSV(pixel, hsv)
                if (isFruitColor(hsv)) {
                    hits.add(Pair(x, y))
                }
            }
        }

        return clusterHits(hits)
    }

    /**
     * Checks whether an HSV color matches a typical Fruit Ninja fruit.
     * Fruits are highly saturated and bright — bombs are black.
     */
    private fun isFruitColor(hsv: FloatArray): Boolean {
        val hue = hsv[0]        // 0–360
        val sat = hsv[1]        // 0–1
        val value = hsv[2]      // 0–1

        // Must be vivid (high saturation & brightness) — avoids background/shadows
        if (sat < 0.45f || value < 0.35f) return false

        // Reject very dark colors (bombs are ~black)
        if (value < 0.20f) return false

        // Fruit hue ranges:
        // Red/Strawberry:  0–15  and  345–360
        // Orange/Peach:    15–45
        // Yellow/Lemon:    45–70
        // Green/Kiwi/Lime: 70–160
        // Purple/Plum:     270–320
        // Pink/Watermelon: 320–345

        return hue <= 15f || hue >= 345f          // reds
                || (hue in 15f..160f)             // orange, yellow, green
                || (hue in 270f..345f)            // purple, pink
    }

    /**
     * Groups nearby hit pixels into clusters and returns each cluster's centroid.
     */
    private fun clusterHits(hits: List<Pair<Int, Int>>): List<FruitTarget> {
        if (hits.isEmpty()) return emptyList()

        val assigned = BooleanArray(hits.size)
        val clusters = mutableListOf<List<Pair<Int, Int>>>()

        for (i in hits.indices) {
            if (assigned[i]) continue
            val cluster = mutableListOf(hits[i])
            assigned[i] = true
            for (j in i + 1 until hits.size) {
                if (assigned[j]) continue
                val dx = (hits[i].first - hits[j].first).toFloat()
                val dy = (hits[i].second - hits[j].second).toFloat()
                if (Math.sqrt((dx * dx + dy * dy).toDouble()) < CLUSTER_RADIUS) {
                    cluster.add(hits[j])
                    assigned[j] = true
                }
            }
            if (cluster.size >= MIN_CLUSTER_SIZE) {
                clusters.add(cluster)
            }
        }

        return clusters.map { cluster ->
            val cx = cluster.map { it.first }.average().toFloat()
            val cy = cluster.map { it.second }.average().toFloat()
            FruitTarget(cx, cy)
        }
    }
}
