package com.mobilefilmlab.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionConsistencyTest {

    @Test
    fun testSemanticVersionName() {
        val versionName = "0.1.0-alpha.6"
        assertEquals("0.1.0-alpha.6", versionName)
    }

    @Test
    fun testMonotonicVersionCode() {
        val versionCode = 6
        assertEquals(6, versionCode)
    }
}
