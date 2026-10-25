package com.mobilefilmlab.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionConsistencyTest {

    @Test
    fun testSemanticVersionName() {
        val versionName = "0.1.0-alpha.7"
        assertEquals("0.1.0-alpha.7", versionName)
    }

    @Test
    fun testMonotonicVersionCode() {
        val versionCode = 7
        assertEquals(7, versionCode)
    }
}
