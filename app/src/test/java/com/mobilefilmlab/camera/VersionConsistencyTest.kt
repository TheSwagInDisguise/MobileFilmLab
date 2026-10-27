package com.mobilefilmlab.camera

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VersionConsistencyTest {
    @Test fun productionVersionIsAlphaNine() {
        assertEquals("0.1.0-alpha.9",BuildConfig.VERSION_NAME)
        assertEquals(9,BuildConfig.VERSION_CODE)
        val gradle=File("build.gradle.kts").takeIf { it.exists() } ?: File("app/build.gradle.kts")
        val config=gradle.readText()
        assertTrue(config.contains("applicationId = \"com.mobilefilmlab.camera\""))
        assertTrue(config.contains("versionCode = ${BuildConfig.VERSION_CODE}"))
        assertTrue(config.contains("versionName = \"${BuildConfig.VERSION_NAME}\""))
    }
}
