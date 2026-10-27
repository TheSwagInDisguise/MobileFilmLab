package com.mobilefilmlab.camera.data.importer

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mobilefilmlab.camera.data.model.*

/** Versioned, bounded interchange. Legacy array-only JSON remains readable. */
object RecipeJsonSerializer {
    const val MAX_BYTES=262144
    const val MAX_RECIPES=100
    private val gson=GsonBuilder().setPrettyPrinting().create()
    fun toJson(recipes: List<Recipe>): String {
        require(recipes.size<=MAX_RECIPES)
        recipes.forEach { validate(it) }
        return gson.toJson(mapOf("type" to "mobilefilmlab.recipe","version" to 1,"recipes" to recipes))
    }
    fun fromJson(json: String): Result<List<Recipe>> = runCatching {
        require(json.toByteArray(Charsets.UTF_8).size<=MAX_BYTES) { "Recipe file is too large (256KB limit)" }
        val root=JsonParser.parseString(json)
        val array=if(root.isJsonArray) root.asJsonArray else {
            require(root.isJsonObject) { "Expected a recipe document" }
            val o=root.asJsonObject
            require(o.get("type")?.asString=="mobilefilmlab.recipe") { "Not a MobileFilmLab recipe document" }
            require(o.get("version")?.asInt==1) { "Unsupported recipe format version" }
            require(o.get("recipes")?.isJsonArray==true) { "Missing recipes" }
            o.getAsJsonArray("recipes")
        }
        require(array.size() in 1..MAX_RECIPES) { "Import 1–100 recipes at a time" }
        array.map {
            require(it.isJsonObject) { "Invalid recipe entry" }
            val o=it.asJsonObject
            require(o.has("id") && o.has("name")) { "Recipe needs an ID and name" }
            val recipe=gson.fromJson(o,Recipe::class.java)
            validate(recipe)
            recipe
        }
    }
    fun validate(r: Recipe) {
        require(!r.id.isNullOrBlank() && r.id.length<=100) { "Invalid recipe ID" }
        require(!r.name.isNullOrBlank() && r.name.length<=80 && r.name.none { it<' ' }) { "Recipe name must be 1–80 printable characters" }
        requireNotNull(r.group) { "Unknown recipe group" }
        require(r.style in listOf(1,2,3,4,5,6,7,8,9,10,11,12,14)) { "Invalid creative style" }
        require(r.sat in -3..3 && r.con in -3..3 && r.sharp in -3..3) { "Invalid style adjustment" }
        require(r.wbMode in listOf(WbMode.KEEP,WbMode.AUTO,WbMode.KELVIN)) { "Invalid white balance mode" }
        require(r.wbMode!=WbMode.KELVIN || r.kelvin in 2500..9900) { "Invalid Kelvin value" }
        require(r.ab in -7..7 && r.gm in -7..7 && r.ev in -15..15) { "Invalid tint or exposure" }
        require(r.dro in 0..6 && r.pe in 0..13 && r.sub in 0..20) { "Invalid effect parameter" }
        require(r.puckTone.isFinite() && r.puckColor.isFinite() && r.puckTone in -100f..100f && r.puckColor in -100f..100f) { "Invalid style coordinates" }
        require(r.version==1 && r.notes != null && r.notes.length<=4000) { "Invalid recipe version or notes" }
    }
}
