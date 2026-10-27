package com.mobilefilmlab.camera

import android.view.ViewGroup
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** UI/lifecycle smoke only: deliberately does not pretend to test a real ColorOS camera. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActivitySmokeTest {
    @Test(timeout=60000) fun activityCreatesComposeCameraUiAndClosesWithoutCameraHardware() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        val activity=controller.get()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        val content=activity.findViewById<ViewGroup>(android.R.id.content)
        assertTrue(content.childCount>0)
        controller.pause().stop().destroy()
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }
}
