package com.example.fruitslicer

import android.graphics.Bitmap
import android.graphics.Color

object FruitDetector {

    private const val SAMPLE_STEP = 8
    private const val MIN_CLUSTER_SIZE = 4
    private const val CLUSTER_RADIUS = 100

    data class FruitTarget(val x: Float, val y: Float)

    fun detect(bitmap: Bitmap): List<FruitTarget> {
        val width = bitmap.width
        val height = bitmap.height
        val startY = (height * 0.10).toInt()
        val endY   = (height * 0.92).toInt()

        val hits = mutableListOf<Pair<Int, Int>>()
        val hsv = FloatArray(3)

        for (y in startY until endY step SAMPLE_STEP) {
            for (x in 0 until width step SAMPLE_STEP) {
                val pixel = bitmap.getPixel(x, y)
                Color.colorToHSV(pixel, hsv)
                if (isFruitColor(hsv, pixel)) hits.add(x to y)
            }
        }

        return clusterHits(hits)
    }

    private fun isFruitColor(hsv: FloatArray, pixel: Int): Boolean {
        val sat   = hsv[1]
        val value = hsv[2]
        val hue   = hsv[0]

        // Must be vivid and bright — not black (bomb) or white (background)
        if (sat < 0.30f || value < 0.25f) return false
        val avg = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
        if (avg < 30 || avg > 245) return false

        // Fruit hue ranges
        return hue <= 20f || hue >= 340f       // red / strawberry
            || hue in 20f..85f                 // orange / yellow / lemon
            || hue in 85f..175f               // green / kiwi / lime
            || hue in 260f..340f              // purple / plum / dragonfruit
    }

    private fun clusterHits(hits: List<Pair<Int, Int>>): List<FruitTarget> {
        if (hits.isEmpty()) return emptyList()
        val assigned = BooleanArray(hits.size)
        val result = mutableListOf<FruitTarget>()

        for (i in hits.indices) {
            if (assigned[i]) continue
            val cluster = mutableListOf(hits[i])
            assigned[i] = true
            for (j in i + 1 until hits.size) {
                if (assigned[j]) continue
                val dx = (hits[i].first  - hits[j].first).toFloat()
                val dy = (hits[i].second - hits[j].second).toFloat()
                if (dx * dx + dy * dy < CLUSTER_RADIUS * CLUSTER_RADIUS) {
                    cluster.add(hits[j])
                    assigned[j] = true
                }
            }
            if (cluster.size >= MIN_CLUSTER_SIZE) {
                result.add(FruitTarget(
                    cluster.map { it.first  }.average().toFloat(),
                    cluster.map { it.second }.average().toFloat()
                ))
            }
        }
        return result
    }
}
