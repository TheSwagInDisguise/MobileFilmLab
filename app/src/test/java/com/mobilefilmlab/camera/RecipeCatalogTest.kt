package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.data.model.CreativeStyle
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import com.mobilefilmlab.camera.data.model.RecipeGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeCatalogTest {

    @Test
    fun testFullUpstreamCataloguePlusThreeCamera2Baselines() {
        val total = RecipeCatalog.getUpstream77Recipes().size
        assertEquals("Upstream definitions must still contain exactly 77 recipes", 77, total)
        assertEquals(80, RecipeCatalog.ALL_RECIPES.size)
        assertEquals(3, RecipeCatalog.getByGroup(RecipeGroup.OPPO).size)
    }

    @Test
    fun testFactoryRecipeIsFirst() {
        val first = RecipeCatalog.ALL_RECIPES.first()
        assertEquals("Oppo Natural (Camera2)", first.name)
        assertEquals(RecipeGroup.OPPO, first.group)
        assertEquals(CreativeStyle.STD, first.style)
        assertEquals(0, first.sat)
        assertEquals(0, first.con)
        assertEquals(0, first.sharp)
        assertEquals(first.id, RecipeCatalog.getFactory().id)
        assertNotNull(RecipeCatalog.getById("sony_factory_st"))
    }

    @Test
    fun testAllRecipeIdsAreUnique() {
        val ids = RecipeCatalog.ALL_RECIPES.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("All built-in recipe IDs must be unique", ids.size, uniqueIds.size)
    }

    @Test
    fun testNamesAreUniqueWithinGroups() {
        for (group in RecipeGroup.values()) {
            if (group == RecipeGroup.CUSTOM) continue
            val recipesInGroup = RecipeCatalog.getByGroup(group)
            val names = recipesInGroup.map { it.name }
            val uniqueNames = names.toSet()
            assertEquals("Recipe names in group ${group.name} must be unique", names.size, uniqueNames.size)
        }
    }

    @Test
    fun testGroupCountsMatchUpstream() {
        assertEquals(8, RecipeCatalog.getByGroup(RecipeGroup.SONY).size)
        assertEquals(16, RecipeCatalog.getByGroup(RecipeGroup.FUJI_SIM).size)
        assertEquals(5, RecipeCatalog.getByGroup(RecipeGroup.FUJI_FILM).size)
        assertEquals(14, RecipeCatalog.getByGroup(RecipeGroup.KODAK).size)
        assertEquals(4, RecipeCatalog.getByGroup(RecipeGroup.CINE).size)
        assertEquals(8, RecipeCatalog.getByGroup(RecipeGroup.RICOH_GR).size)
        assertEquals(4, RecipeCatalog.getByGroup(RecipeGroup.LEICA).size)
        assertEquals(1, RecipeCatalog.getByGroup(RecipeGroup.HASSELBLAD).size)
        assertEquals(5, RecipeCatalog.getByGroup(RecipeGroup.CANON_NIKON).size)
        assertEquals(4, RecipeCatalog.getByGroup(RecipeGroup.PANA_OLYMPUS).size)
        assertEquals(3, RecipeCatalog.getByGroup(RecipeGroup.OTHER_STOCKS).size)
        assertEquals(5, RecipeCatalog.getByGroup(RecipeGroup.ILFORD).size)
    }

    @Test
    fun testParameterRanges() {
        for (recipe in RecipeCatalog.ALL_RECIPES) {
            assertTrue("Sat must be -3..+3 for ${recipe.name}", recipe.sat in -3..3)
            assertTrue("Con must be -3..+3 for ${recipe.name}", recipe.con in -3..3)
            assertTrue("Sharp must be -3..+3 for ${recipe.name}", recipe.sharp in -3..3)
            assertTrue("AB must be -7..+7 for ${recipe.name}", recipe.ab in -7..7)
            assertTrue("GM must be -7..+7 for ${recipe.name}", recipe.gm in -7..7)
            assertTrue("EV must be -15..+15 for ${recipe.name}", recipe.ev in -15..15)
            assertTrue("DRO must be 0..6 for ${recipe.name}", recipe.dro in 0..6)
            assertTrue("PE must be 0..13 for ${recipe.name}", recipe.pe in 0..13)
        }
    }
}
