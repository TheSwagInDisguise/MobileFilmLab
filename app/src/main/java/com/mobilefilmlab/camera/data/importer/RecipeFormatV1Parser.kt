package com.mobilefilmlab.camera.data.importer

import com.mobilefilmlab.camera.data.model.CreativeStyle
import com.mobilefilmlab.camera.data.model.DroMode
import com.mobilefilmlab.camera.data.model.PictureEffect
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.model.RecipeGroup
import com.mobilefilmlab.camera.data.model.WbMode
import java.util.UUID

/**
 * Full parser and serializer for Upstream Recipe Lab YAML Format 1.
 * Compatible with memory card .YML files produced by recipe-lab-sony-pmca.
 */
object RecipeFormatV1Parser {

    fun parseYaml(content: String): Result<Recipe> = runCatching {
        val lines = content.lines()
        val kv = mutableMapOf<String, String>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                var value = parts[1].trim()
                if (value.contains("#")) {
                    value = value.substringBefore("#").trim()
                }
                if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
                    value = value.substring(1, value.length - 1)
                }
                kv[key] = value
            }
        }

        val name = kv["name"] ?: "Imported Recipe"
        val styleStr = kv["style"] ?: "standard"
        val styleIndex = CreativeStyle.NAMES.indexOf(styleStr).let { if (it > 0) it else CreativeStyle.STD }

        val sat = kv["saturation"]?.toIntOrNull()?.coerceIn(-3, 3) ?: 0
        val con = kv["contrast"]?.toIntOrNull()?.coerceIn(-3, 3) ?: 0
        val sharp = kv["sharpness"]?.toIntOrNull()?.coerceIn(-3, 3) ?: 0

        val effectStr = kv["effect"] ?: "off"
        val peIndex = PictureEffect.KEYS.indexOf(effectStr).let { if (it >= 0) it else PictureEffect.OFF }

        var wbMode = WbMode.AUTO
        var kelvin = 0
        val wbStr = kv["white-balance"]?.lowercase() ?: "auto"
        when {
            wbStr == "auto" -> wbMode = WbMode.AUTO
            wbStr == "keep" -> wbMode = WbMode.KEEP
            wbStr.endsWith("k") -> {
                wbMode = WbMode.KELVIN
                kelvin = wbStr.removeSuffix("k").toIntOrNull()?.coerceIn(2500, 9900) ?: 5600
            }
            wbStr.toIntOrNull() != null -> {
                wbMode = WbMode.KELVIN
                kelvin = wbStr.toInt().coerceIn(2500, 9900)
            }
        }

        val ab = kv["amber-blue"]?.toIntOrNull()?.coerceIn(-7, 7) ?: 0
        val gm = kv["green-magenta"]?.toIntOrNull()?.coerceIn(-7, 7) ?: 0

        val evStr = kv["exposure"] ?: "0"
        val ev = parseEv(evStr)

        val droStr = kv["dro"]?.lowercase() ?: "auto"
        val dro = when (droStr) {
            "off" -> DroMode.OFF
            "auto" -> DroMode.AUTO
            else -> droStr.toIntOrNull()?.coerceIn(1, 5) ?: DroMode.AUTO
        }

        Recipe(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = name,
            group = RecipeGroup.CUSTOM,
            style = styleIndex,
            sat = sat,
            con = con,
            sharp = sharp,
            wbMode = wbMode,
            kelvin = kelvin,
            ab = ab,
            gm = gm,
            pe = peIndex,
            ev = ev,
            dro = dro,
            sub = 0,
            isCustom = true
        )
    }

    fun exportToYaml(recipe: Recipe): String {
        val sb = StringBuilder()
        sb.append("# Recipe Lab custom recipe format 1 (MobileFilmLab)\n")
        sb.append("format: 1\n")
        sb.append("name: \"${recipe.name}\"\n")
        sb.append("style: ${CreativeStyle.NAMES.getOrNull(recipe.style) ?: "standard"}\n")
        sb.append("saturation: ${if (recipe.sat > 0) "+${recipe.sat}" else recipe.sat.toString()}\n")
        sb.append("contrast: ${if (recipe.con > 0) "+${recipe.con}" else recipe.con.toString()}\n")
        sb.append("sharpness: ${if (recipe.sharp > 0) "+${recipe.sharp}" else recipe.sharp.toString()}\n")
        sb.append("effect: ${PictureEffect.KEYS.getOrNull(recipe.pe) ?: "off"}\n")
        val wb = when (recipe.wbMode) {
            WbMode.KELVIN -> "${recipe.kelvin}K"
            WbMode.KEEP -> "keep"
            else -> "auto"
        }
        sb.append("white-balance: $wb\n")
        sb.append("amber-blue: ${if (recipe.ab > 0) "+${recipe.ab}" else recipe.ab.toString()}\n")
        sb.append("green-magenta: ${if (recipe.gm > 0) "+${recipe.gm}" else recipe.gm.toString()}\n")
        sb.append("exposure: ${Recipe.evLabel(recipe.ev)}\n")
        sb.append("dro: ${if (recipe.dro == DroMode.AUTO) "auto" else if (recipe.dro == 0) "off" else recipe.dro.toString()}\n")
        return sb.toString()
    }

    private fun parseEv(v: String): Int {
        val clean = v.trim().replace("+", "")
        val d = clean.toDoubleOrNull() ?: 0.0
        return Math.round(d * 3.0).toInt().coerceIn(-15, 15)
    }
}
