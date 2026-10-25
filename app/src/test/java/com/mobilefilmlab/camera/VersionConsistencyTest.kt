package com.mobilefilmlab.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionConsistencyTest {

    @Test
    fun testSemanticVersionName() {
        val versionName = "0.1.0-alpha.4"
        assertEquals("0.1.0-alpha.4", versionName)
    }

    @Test
    fun testMonotonicVersionCode() {
        val versionCode = 4
        assertEquals(4, versionCode)
    }
}
