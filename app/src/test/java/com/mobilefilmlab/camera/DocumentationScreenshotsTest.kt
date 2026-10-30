package com.mobilefilmlab.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.ViewModelProvider
import com.mobilefilmlab.camera.camera.CameraEngine
import com.mobilefilmlab.camera.data.model.*
import com.mobilefilmlab.camera.ui.components.LocalDocumentationScene
import com.mobilefilmlab.camera.ui.components.MainCameraScreen
import com.mobilefilmlab.camera.ui.theme.MobileFilmLabTheme
import com.mobilefilmlab.camera.ui.viewmodel.CameraMode
import com.mobilefilmlab.camera.ui.viewmodel.CameraViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.time.Duration

/**
 * Native Android/Compose UI renders, NOT handset captures or camera-quality evidence.
 * The real screen, controls, drawer and colour pipeline are rendered unchanged.
 * The only substituted inputs are a labelled AI demo scene, demo camera metadata,
 * and ordinary saved custom-recipe state. No camera hardware is simulated as tested.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w432dp-h960dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentationScreenshotsTest {
    @Test(timeout = 90000) fun rendersActualAutoProAndRecipeUiForDocumentation() {
        val root = File("..").takeIf { File(it, "docs/assets/demo-scene.png").isFile } ?: File(".")
        val scene = BitmapFactory.decodeFile(File(root, "docs/assets/demo-scene.png").absolutePath)
        checkNotNull(scene) { "Missing documented demonstration image" }
        val output = File(root, "app/build/outputs/documentation-screenshots").apply { mkdirs() }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        val activity = controller.get()
        val vm = ViewModelProvider(activity)[CameraViewModel::class.java]
        vm.onPause() // No real camera is used or claimed by these renders.
        shadowOf(Looper.getMainLooper()).idle()
        val engine = vm.cameraEngine
        val spec = CameraSpec("documentation", true, false, focalLengthMm = 6f,
            equivalentFocalLength35mm = 23, sensorWidthMm = 9f, sensorHeightMm = 12f,
            apertureFNumber = 1.8f, activeArrayWidth = scene.width, activeArrayHeight = scene.height,
            maxResolutionMp = scene.width * scene.height / 1_000_000f, hasRawSupport = true,
            lensType = LensType.WIDE, displayName = "Documentation demo only", manualSensor = true, manualWb = true, focusModes = listOf("AF-C","AF-S","MACRO"), rawSizes = listOf(StreamSize(scene.width,scene.height)), rawUnavailableReason = "")
        // Private test-only state injection; there is no shipping demo switch or fake camera implementation.
        flow<List<CameraSpec>>(engine, "_cameraSpecs").value = listOf(spec.copy(cameraId="demo-ultra",equivalentFocalLength35mm=14,hasRawSupport=false,rawSizes=emptyList(),rawUnavailableReason="Illustrative JPEG-only route"),spec, spec.copy(cameraId="demo-tele",equivalentFocalLength35mm=83,lensType=LensType.TELEPHOTO))
        flow<CameraSpec?>(engine, "_activeCameraSpec").value = spec
        flow<Boolean>(engine, "_isCameraStreaming").value = true
        flow<String?>(engine, "_cameraError").value = null
        vm.updateSettings(com.mobilefilmlab.camera.settings.CaptureSettings())
        vm.framingConfig.value = FramingConfig.create(LensType.WIDE, false, spec)
        vm.userNotice.value = null
        val warm = Recipe("custom_documentation_warm", "Coastal Gold", RecipeGroup.CUSTOM,
            sat = 1, con = 1, ab = 1, puckTone = 8f, puckColor = 18f, isCustom = true)
        val mono = warm.copy(id = "custom_documentation_mono", name = "Soft Monochrome", style = CreativeStyle.MONO,
            sat = 0, con = 0, ab = 0, puckTone = -8f, puckColor = 0f)
        vm.repository.saveCustomRecipe(warm)
        vm.repository.toggleFavourite(warm)
        vm.repository.saveCustomRecipe(mono)
        vm.repository.toggleFavourite(mono)
        vm.selectRecipe(RecipeCatalog.getDefaultRecipe())
        vm.lastCapturedThumbnail.value = Bitmap.createScaledBitmap(scene, 160, 160, true)
        activity.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalDocumentationScene provides scene) {
                MobileFilmLabTheme { MainCameraScreen(vm) }
            }
        }
        fun settle() {
            activity.window.decorView.measure(exact(864), exact(1920))
            activity.window.decorView.layout(0, 0, 864, 1920)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600))
            activity.window.decorView.measure(exact(864), exact(1920))
            activity.window.decorView.layout(0, 0, 864, 1920)
        }
        fun capture(name: String) {
            settle()
            val bitmap = Bitmap.createBitmap(864, 1920, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.BLACK)
            val global = ReflectionHelpers.callStaticMethod<Any>(Class.forName("android.view.WindowManagerGlobal"), "getInstance")
            val roots = ReflectionHelpers.getField<List<View>>(global, "mViews").toList()
            assertTrue("Actual Android window must be present", roots.isNotEmpty())
            roots.filter { it.visibility == View.VISIBLE }.forEach { view ->
                if (view.width == 0 || view.height == 0) { view.measure(exact(864), exact(1920)); view.layout(0, 0, 864, 1920) }
                canvas.save()
                val position = IntArray(2)
                view.getLocationOnScreen(position)
                // Headless WindowManager can report (0,0) for floating windows. Honour the
                // real window gravity instead of moving the app's AlertDialog to the top.
                val params=view.layoutParams as? WindowManager.LayoutParams
                val x=when(params?.gravity?.and(Gravity.HORIZONTAL_GRAVITY_MASK)) {
                    Gravity.CENTER_HORIZONTAL -> (864-view.width)/2+(params.x)
                    Gravity.RIGHT -> 864-view.width-(params.x)
                    else -> position[0]
                }
                val y=when(params?.gravity?.and(Gravity.VERTICAL_GRAVITY_MASK)) {
                    Gravity.CENTER_VERTICAL -> (1920-view.height)/2+(params.y)
                    Gravity.BOTTOM -> 1920-view.height-(params.y)
                    else -> position[1]
                }
                canvas.translate(x.toFloat(), y.toFloat())
                view.draw(canvas)
                canvas.restore()
            }
            // A text-only settings Surface can have a nearly uniform centre column.
            // Sample across the whole native window, not only down its centreline.
            val samples = (72..792 step 48).flatMap { x -> (100..1800 step 40).map { y -> bitmap.getPixel(x,y) } }.toSet()
            assertTrue("Render unexpectedly blank: $name", samples.size > 6)
            File(output, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        try {
            capture("auto.png")
            vm.mode.value = CameraMode.PRO
            vm.iso.value = 200; vm.exposureTimeNs.value = 8_000_000L; vm.whiteBalanceKelvin.value = 5600
            vm.selectRecipe(warm)
            capture("pro.png")
            vm.isRecipeSheetOpen.value = true
            capture("recipes.png")
            vm.isRecipeSheetOpen.value = false
            vm.isFormatPickerOpen.value = true
            capture("formats.png")
            vm.isFormatPickerOpen.value = false
            vm.isSettingsOpen.value = true
            capture("settings.png")
        } finally {
            vm.isRecipeSheetOpen.value = false
            vm.isFormatPickerOpen.value = false
            vm.isSettingsOpen.value = false
            controller.pause().stop().destroy()
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertTrue(File(output, "recipes.png").length() > 20_000)
        assertTrue(File(output, "formats.png").length() > 20_000)
        assertTrue(File(output, "settings.png").length() > 20_000)
    }
    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
    private fun <T> flow(engine: CameraEngine, name: String): MutableStateFlow<T> = ReflectionHelpers.getField(engine, name)
}
