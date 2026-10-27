package com.mobilefilmlab.camera.camera

/** Uses actual HAL sizes only. No invented fallback buffers or automatic upscaling. */
object ResolutionSelector {
    data class Candidate(val width: Int, val height: Int, val maximumMode: Boolean = false) {
        val area: Long get() = width.toLong() * height
    }
    fun select(candidates: List<Candidate>, limit: Long = 55_000_000L): Candidate? {
        val allowed = candidates.filter { it.width > 0 && it.height > 0 && it.area <= limit }
        return allowed.firstOrNull { minOf(it.width, it.height) == 4596 && maxOf(it.width, it.height) == 6120 }
            ?: allowed.maxByOrNull { it.area }
            ?: candidates.filter { it.width > 0 && it.height > 0 }.minByOrNull { it.area }
    }
}
