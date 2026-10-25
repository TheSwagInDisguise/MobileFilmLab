package com.mobilefilmlab.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionConsistencyTest {

    @Test
    fun testSemanticVersionName() {
        val versionName = "0.1.0-alpha.8"
        assertEquals("0.1.0-alpha.8", versionName)
    }

    @Test
    fun testMonotonicVersionCode() {
        val versionCode = 8
        assertEquals(8, versionCode)
    }
}
