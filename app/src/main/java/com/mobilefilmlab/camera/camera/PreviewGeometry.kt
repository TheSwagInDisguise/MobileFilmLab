package com.mobilefilmlab.camera.camera

/** Shared by touch metering and preview: centre-fill, then undo the sensor's clockwise rotation. */
object PreviewGeometry {
    data class Point(val x: Float, val y: Float)
    fun mapToSensor(x: Float, y: Float, viewAspect: Float, sensorAspect: Float, rotation: Int): Point {
        val uprightAspect = if (rotation % 180 == 0) sensorAspect else 1f / sensorAspect
        var px = x.coerceIn(0f, 1f)
        var py = y.coerceIn(0f, 1f)
        if (viewAspect < uprightAspect) px = 0.5f + (px - 0.5f) * (viewAspect / uprightAspect)
        else py = 0.5f + (py - 0.5f) * (uprightAspect / viewAspect)
        return when ((rotation % 360 + 360) % 360) {
            90 -> Point(py, 1f - px)
            180 -> Point(1f - px, 1f - py)
            270 -> Point(1f - py, px)
            else -> Point(px, py)
        }
    }
}
