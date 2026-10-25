package com.mobilefilmlab.camera.data.importer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mobilefilmlab.camera.data.model.Recipe

object RecipeJsonSerializer {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(recipes: List<Recipe>): String {
        return gson.toJson(recipes)
    }

    fun fromJson(json: String): Result<List<Recipe>> = runCatching {
        val type = object : TypeToken<List<Recipe>>() {}.type
        gson.fromJson(json, type)
    }
}
