package com.mobilefilmlab.camera.data.model

import java.util.UUID

/**
 * Creative Style constants ported from upstream Recipe Lab.
 */
object CreativeStyle {
    const val STD = 1
    const val VIVID = 2
    const val NEUTRAL = 3
    const val PORTRAIT = 4
    const val LANDSCAPE = 5
    const val MONO = 6
    const val CLEAR = 7
    const val DEEP = 8
    const val LIGHT = 9
    const val SUNSET = 10
    const val NIGHT = 11
    const val AUTUMN = 12
    const val SEPIA = 14

    val NAMES = arrayOf(
        null, "standard", "vivid", "neutral", "portrait", "landscape",
        "mono", "clear", "deep", "light", "sunset", "night", "red-leaves", null, "sepia"
    )

    val LABELS = arrayOf(
        "?", "Standard", "Vivid", "Neutral", "Portrait", "Landscape",
        "B&W", "Clear", "Deep", "Light", "Sunset", "Night", "Autumn", "?", "Sepia"
    )
}

/**
 * Picture Effect constants ported from upstream Recipe Lab.
 */
object PictureEffect {
    const val OFF = 0
    const val TOY_CAMERA = 1
    const val POP_COLOR = 2
    const val POSTERIZATION = 3
    const val RETRO_PHOTO = 4
    const val SOFT_HIGH_KEY = 5
    const val PART_COLOR = 6
    const val ROUGH_MONO = 7
    const val SOFT_FOCUS = 8
    const val HDR_ART = 9
    const val RICHTONE_MONO = 10
    const val MINIATURE = 11
    const val ILLUST = 12
    const val WATERCOLOR = 13

    val KEYS = arrayOf(
        "off", "toy-camera", "pop-color", "posterization", "retro-photo",
        "soft-high-key", "part-color", "rough-mono", "soft-focus", "hdr-art",
        "richtone-mono", "miniature", "illust", "watercolor"
    )

    val LABELS = arrayOf(
        "Off", "Toy", "Pop", "Poster", "Retro",
        "High-key", "Part col", "HC mono", "Soft foc", "HDR art",
        "Rich mono", "Miniature", "Illust", "Watercol"
    )
}

/**
 * White Balance modes from upstream.
 */
object WbMode {
    const val KEEP = 0
    const val AUTO = 1
    const val KELVIN = 14
}

/**
 * Dynamic Range Optimizer (DRO) modes from upstream.
 */
object DroMode {
    const val OFF = 0
    const val LEVEL_1 = 1
    const val LEVEL_2 = 2
    const val LEVEL_3 = 3
    const val LEVEL_4 = 4
    const val LEVEL_5 = 5
    const val AUTO = 6
}

/**
 * Core Recipe model representing both upstream built-in presets and user-created custom recipes.
 */
data class Recipe(
    val id: String,
    val name: String,
    val group: RecipeGroup,
    // Original Sony parameters
    val style: Int = CreativeStyle.STD,
    val sat: Int = 0,               // -3 .. +3
    val con: Int = 0,               // -3 .. +3
    val sharp: Int = 0,             // -3 .. +3
    val wbMode: Int = WbMode.AUTO,  // 0 = keep, 1 = auto, 14 = kelvin
    val kelvin: Int = 0,            // 2500K .. 9900K (when wbMode == 14)
    val ab: Int = 0,                // -7 (blue) .. +7 (amber)
    val gm: Int = 0,                // -7 (magenta) .. +7 (green)
    val pe: Int = PictureEffect.OFF,// 0 .. 13
    val ev: Int = 0,                // -15 .. +15 in 1/3 EV steps (-5.0 .. +5.0)
    val dro: Int = DroMode.AUTO,    // 0 = off, 1..5 = level, 6 = auto
    val sub: Int = 0,               // sub-parameter for effect
    // 2D Photographic Style Cartesian Coordinates (-100 .. +100)
    val puckTone: Float = 0f,       // Y-axis: -100 (muted) .. +100 (contrast)
    val puckColor: Float = 0f,      // X-axis: -100 (cool) .. +100 (warm)
    // Metadata
    val isFavourite: Boolean = false,
    val isCustom: Boolean = false,
    val version: Int = 1,
    val notes: String = ""
) {
    fun isEffect(): Boolean = pe != PictureEffect.OFF

    val isMonochrome: Boolean
        get() = style == CreativeStyle.MONO || pe == PictureEffect.ROUGH_MONO || pe == PictureEffect.RICHTONE_MONO

    /**
     * One-line summary matching upstream Recipe Lab format:
     * e.g. "Neutral -3/-1 A1 G0 EV +0.7"
     */
    fun summary(): String {
        val s = StringBuilder()
        if (pe != PictureEffect.OFF) {
            s.append(PictureEffect.LABELS.getOrElse(pe) { "PE $pe" })
        } else {
            val styleName = CreativeStyle.LABELS.getOrElse(style) { "Style $style" }
            s.append(styleName)
            s.append("  ")
            s.append(if (sat > 0) "+$sat" else "$sat")
            s.append("/")
            s.append(if (con > 0) "+$con" else "$con")
        }
        if (ev != 0) {
            s.append("  ").append(evLabel(ev))
        }
        if (dro != DroMode.AUTO) {
            s.append("  DRO ").append(if (dro == 0) "Off" else "Lv$dro")
        }
        if (wbMode == WbMode.KELVIN && kelvin > 0) {
            s.append("  ").append(kelvin).append("K")
        }
        if (ab != 0) {
            s.append("  ").append(if (ab > 0) "A$ab" else "B" + (-ab))
        }
        if (gm != 0) {
            s.append("  ").append(if (gm > 0) "G$gm" else "M" + (-gm))
        }
        return s.toString()
    }

    companion object {
        fun evLabel(ev: Int): String {
            if (ev == 0) return "0"
            val abs = Math.abs(ev)
            val whole = abs / 3
            val frac = when (abs % 3) {
                0 -> ".0"
                1 -> ".3"
                else -> ".7"
            }
            return (if (ev > 0) "+" else "-") + whole + frac
        }

        fun createCustom(
            name: String,
            baseRecipe: Recipe,
            puckTone: Float,
            puckColor: Float
        ): Recipe {
            return baseRecipe.copy(
                id = "custom_" + UUID.randomUUID().toString().take(8),
                name = name.trim(),
                group = RecipeGroup.CUSTOM,
                puckTone = puckTone,
                puckColor = puckColor,
                isCustom = true,
                isFavourite = false
            )
        }
    }
}
