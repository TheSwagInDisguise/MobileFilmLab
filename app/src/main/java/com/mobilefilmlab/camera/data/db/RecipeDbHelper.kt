package com.mobilefilmlab.camera.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.model.RecipeGroup

class RecipeDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_RECIPES (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                group_name TEXT NOT NULL,
                style INTEGER NOT NULL,
                sat INTEGER NOT NULL,
                con INTEGER NOT NULL,
                sharp INTEGER NOT NULL,
                wb_mode INTEGER NOT NULL,
                kelvin INTEGER NOT NULL,
                ab INTEGER NOT NULL,
                gm INTEGER NOT NULL,
                pe INTEGER NOT NULL,
                ev INTEGER NOT NULL,
                dro INTEGER NOT NULL,
                sub INTEGER NOT NULL,
                puck_tone REAL NOT NULL,
                puck_color REAL NOT NULL,
                is_favourite INTEGER NOT NULL,
                is_custom INTEGER NOT NULL,
                version INTEGER NOT NULL,
                notes TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_FAVOURITES (
                recipe_id TEXT PRIMARY KEY
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECIPES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVOURITES")
        onCreate(db)
    }

    fun getAllCustomRecipes(): List<Recipe> {
        val list = mutableListOf<Recipe>()
        val db = readableDatabase
        val cursor = db.query(TABLE_RECIPES, null, "is_custom = 1", null, null, null, "created_at DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Recipe(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        group = RecipeGroup.CUSTOM,
                        style = it.getInt(it.getColumnIndexOrThrow("style")),
                        sat = it.getInt(it.getColumnIndexOrThrow("sat")),
                        con = it.getInt(it.getColumnIndexOrThrow("con")),
                        sharp = it.getInt(it.getColumnIndexOrThrow("sharp")),
                        wbMode = it.getInt(it.getColumnIndexOrThrow("wb_mode")),
                        kelvin = it.getInt(it.getColumnIndexOrThrow("kelvin")),
                        ab = it.getInt(it.getColumnIndexOrThrow("ab")),
                        gm = it.getInt(it.getColumnIndexOrThrow("gm")),
                        pe = it.getInt(it.getColumnIndexOrThrow("pe")),
                        ev = it.getInt(it.getColumnIndexOrThrow("ev")),
                        dro = it.getInt(it.getColumnIndexOrThrow("dro")),
                        sub = it.getInt(it.getColumnIndexOrThrow("sub")),
                        puckTone = it.getFloat(it.getColumnIndexOrThrow("puck_tone")),
                        puckColor = it.getFloat(it.getColumnIndexOrThrow("puck_color")),
                        isFavourite = it.getInt(it.getColumnIndexOrThrow("is_favourite")) == 1,
                        isCustom = true,
                        version = it.getInt(it.getColumnIndexOrThrow("version")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes")) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun saveRecipe(recipe: Recipe) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", recipe.id)
            put("name", recipe.name)
            put("group_name", recipe.group.name)
            put("style", recipe.style)
            put("sat", recipe.sat)
            put("con", recipe.con)
            put("sharp", recipe.sharp)
            put("wb_mode", recipe.wbMode)
            put("kelvin", recipe.kelvin)
            put("ab", recipe.ab)
            put("gm", recipe.gm)
            put("pe", recipe.pe)
            put("ev", recipe.ev)
            put("dro", recipe.dro)
            put("sub", recipe.sub)
            put("puck_tone", recipe.puckTone)
            put("puck_color", recipe.puckColor)
            put("is_favourite", if (recipe.isFavourite) 1 else 0)
            put("is_custom", if (recipe.isCustom) 1 else 0)
            put("version", recipe.version)
            put("notes", recipe.notes)
            put("created_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_RECIPES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveRecipes(recipes: List<Recipe>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            recipes.forEach { saveRecipe(it) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun deleteRecipe(recipeId: String) {
        val db = writableDatabase
        db.delete(TABLE_RECIPES, "id = ?", arrayOf(recipeId))
    }

    fun getFavouriteIds(): Set<String> {
        val set = mutableSetOf<String>()
        val db = readableDatabase
        val cursor = db.query(TABLE_FAVOURITES, arrayOf("recipe_id"), null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                set.add(it.getString(0))
            }
        }
        return set
    }

    fun setFavourite(recipeId: String, isFavourite: Boolean) {
        val db = writableDatabase
        if (isFavourite) {
            val cv = ContentValues().apply { put("recipe_id", recipeId) }
            db.insertWithOnConflict(TABLE_FAVOURITES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            db.delete(TABLE_FAVOURITES, "recipe_id = ?", arrayOf(recipeId))
        }
    }

    companion object {
        const val DATABASE_NAME = "mobilefilmlab.db"
        const val DATABASE_VERSION = 1
        const val TABLE_RECIPES = "recipes"
        const val TABLE_FAVOURITES = "favourites"
    }
}
