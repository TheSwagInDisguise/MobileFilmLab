package com.mobilefilmlab.camera.camera

/** No pairing by arrival order: RAW, optional JPEG and sensor metadata must share a timestamp. */
internal object SensorFrameMatch {
    fun matches(rawTimestamp: Long, resultTimestamp: Long?, jpegTimestamp: Long?, requireJpeg: Boolean): Boolean =
        rawTimestamp > 0 && resultTimestamp == rawTimestamp && (!requireJpeg || jpegTimestamp == rawTimestamp)
}
