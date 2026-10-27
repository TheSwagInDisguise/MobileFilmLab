package com.mobilefilmlab.camera.data.repository

import android.content.Context
import com.mobilefilmlab.camera.data.db.RecipeDbHelper
import com.mobilefilmlab.camera.data.importer.RecipeJsonSerializer
import com.mobilefilmlab.camera.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class RecipeRepository(context: Context) {
    private val db=RecipeDbHelper(context)
    private val prefs=context.getSharedPreferences("recipe_defaults",Context.MODE_PRIVATE)
    private val _recipes=MutableStateFlow<List<Recipe>>(emptyList())
    val recipes=_recipes.asStateFlow()
    private val _activeRecipe=MutableStateFlow(RecipeCatalog.getDefaultRecipe())
    val activeRecipe=_activeRecipe.asStateFlow()
    init {
        if(!prefs.getBoolean("natural_seeded",false)) {
            db.setFavourite(RecipeCatalog.STOCK_OPPO_NATURAL.id,true)
            prefs.edit().putBoolean("natural_seeded",true).apply()
        }
        refresh()
        _activeRecipe.value=_recipes.value.first { it.id==RecipeCatalog.getDefaultRecipe().id }
    }
    fun refresh() {
        val favs=db.getFavouriteIds()
        _recipes.value=(RecipeCatalog.ALL_RECIPES+db.getAllCustomRecipes()).map { it.copy(isFavourite=it.id in favs) }
        _recipes.value.firstOrNull { it.id==_activeRecipe.value.id }?.let { _activeRecipe.value=it }
    }
    fun setActiveRecipe(recipe: Recipe) { _activeRecipe.value=recipe }
    fun toggleFavourite(recipe: Recipe) { db.setFavourite(recipe.id,!recipe.isFavourite); refresh() }
    fun saveCustomRecipe(recipe: Recipe) { RecipeJsonSerializer.validate(recipe); db.saveRecipe(recipe); refresh(); setActiveRecipe(recipe) }
    fun importRecipes(recipes: List<Recipe>): List<Recipe> {
        recipes.forEach { RecipeJsonSerializer.validate(it) }
        val imported=recipes.map { it.copy(id="custom_"+UUID.randomUUID(),group=RecipeGroup.CUSTOM,isCustom=true,isFavourite=false) }
        db.saveRecipes(imported); refresh(); setActiveRecipe(imported.first())
        return imported
    }
    fun deleteCustomRecipe(id: String) {
        if(!_recipes.value.any { it.id==id && it.isCustom }) return
        db.deleteRecipe(id); db.setFavourite(id,false)
        if(_activeRecipe.value.id==id) _activeRecipe.value=RecipeCatalog.getDefaultRecipe()
        refresh()
    }
    fun getFavourites()=_recipes.value.filter { it.isFavourite }
    fun getByGroup(group: RecipeGroup)=_recipes.value.filter { it.group==group }
    fun close() { db.close() }
}
