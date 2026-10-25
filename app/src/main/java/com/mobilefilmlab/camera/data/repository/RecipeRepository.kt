package com.mobilefilmlab.camera.data.repository

import android.content.Context
import com.mobilefilmlab.camera.data.db.RecipeDbHelper
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import com.mobilefilmlab.camera.data.model.RecipeGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecipeRepository(context: Context) {
    private val dbHelper = RecipeDbHelper(context)

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _activeRecipe = MutableStateFlow<Recipe>(RecipeCatalog.getFactory())
    val activeRecipe: StateFlow<Recipe> = _activeRecipe.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val favIds = dbHelper.getFavouriteIds()
        val customRecipes = dbHelper.getAllCustomRecipes()

        // 77 catalog recipes with updated favourite flags
        val catalogWithFavs = RecipeCatalog.ALL_RECIPES.map { r ->
            r.copy(isFavourite = favIds.contains(r.id))
        }

        // Custom recipes
        val customsWithFavs = customRecipes.map { r ->
            r.copy(isFavourite = favIds.contains(r.id))
        }

        _recipes.value = catalogWithFavs + customsWithFavs
    }

    fun setActiveRecipe(recipe: Recipe) {
        _activeRecipe.value = recipe
    }

    fun toggleFavourite(recipe: Recipe) {
        val newFav = !recipe.isFavourite
        dbHelper.setFavourite(recipe.id, newFav)
        refresh()
        if (_activeRecipe.value.id == recipe.id) {
            _activeRecipe.value = _activeRecipe.value.copy(isFavourite = newFav)
        }
    }

    fun saveCustomRecipe(recipe: Recipe) {
        dbHelper.saveRecipe(recipe)
        refresh()
        setActiveRecipe(recipe)
    }

    fun deleteCustomRecipe(recipeId: String) {
        dbHelper.deleteRecipe(recipeId)
        refresh()
        if (_activeRecipe.value.id == recipeId) {
            _activeRecipe.value = RecipeCatalog.getFactory()
        }
    }

    fun getFavourites(): List<Recipe> = _recipes.value.filter { it.isFavourite }

    fun getByGroup(group: RecipeGroup): List<Recipe> = _recipes.value.filter { it.group == group }
}
