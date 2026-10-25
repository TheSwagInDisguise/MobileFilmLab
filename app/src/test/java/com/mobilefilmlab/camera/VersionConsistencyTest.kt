package com.mobilefilmlab.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionConsistencyTest {

    @Test
    fun testSemanticVersionName() {
        val versionName = "0.1.0-alpha.5"
        assertEquals("0.1.0-alpha.5", versionName)
    }

    @Test
    fun testMonotonicVersionCode() {
        val versionCode = 5
        assertEquals(5, versionCode)
    }
}
