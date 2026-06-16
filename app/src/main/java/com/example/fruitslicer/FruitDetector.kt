package com.example.fruitslicer

import android.graphics.Bitmap
import android.graphics.Color

object FruitDetector {

    private const val SAMPLE_STEP = 8       // finer grid for better detection
    private const val MIN_CLUSTER_SIZE = 4  // lower threshold
    private const val CLUSTER_RADIUS = 100  // wider merge radius

    data class FruitTarget(val x: Float, val y: Float)

    fun detect(bitmap: Bitmap): List<FruitTarget> {
        val width = bitmap.width
        val height = bitmap.height

        // Skip top 10% (HUD) and bottom 8% (UI buttons)
        val startY = (height * 0.10).toInt()
        val endY = (height * 0.92).toInt()

        val hits = mutableListOf<Pair<Int, Int>>()
        val hsv = FloatArray(3)

        for (y in startY until endY step SAMPLE_STEP) {
            for (x in 0 until width step SAMPLE_STEP) {
                val pixel = bitmap.getPixel(x, y)
                Color.colorToHSV(pixel, hsv)
                if (isFruitColor(hsv, pixel)) {
                    hits.add(Pair(x, y))
                }
            }
        }

        return clusterHits(hits)
    }

    private fun isFruitColor(hsv: FloatArray, pixel: Int): Boolean {
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        // Must be vivid and bright
        if (sat < 0.35f || value < 0.30f) return false

        // Skip near-black (bombs) and near-white (background)
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        val brightness = (r + g + b) / 3
        if (brightness < 40 || brightness > 240) return false

        // Fruit hue ranges — reds, oranges, yellows, greens, purples, pinks
        return hue <= 20f || hue >= 340f          // red / strawberry
                || hue in 20f..80f               // orange / yellow / lemon
                || hue in 80f..170f              // green / kiwi / lime / watermelon rind
                || hue in 260f..340f             // purple / plum / dragonfruit
    }

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
                if (dx * dx + dy * dy < CLUSTER_RADIUS * CLUSTER_RADIUS) {
                    cluster.add(hits[j])
                    assigned[j] = true
                }
            }
            if (cluster.size >= MIN_CLUSTER_SIZE) {
                clusters.add(cluster)
            }
        }

        return clusters.map { cluster ->
            FruitTarget(
                cluster.map { it.first }.average().toFloat(),
                cluster.map { it.second }.average().toFloat()
            )
        }
    }
}
