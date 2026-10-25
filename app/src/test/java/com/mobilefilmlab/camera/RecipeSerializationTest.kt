package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.data.importer.RecipeFormatV1Parser
import com.mobilefilmlab.camera.data.importer.RecipeJsonSerializer
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSerializationTest {

    @Test
    fun testJsonRoundTrip() {
        val original = RecipeCatalog.ALL_RECIPES.take(5)
        val json = RecipeJsonSerializer.toJson(original)
        assertTrue(json.isNotEmpty())

        val result = RecipeJsonSerializer.fromJson(json)
        assertTrue(result.isSuccess)
        val restored = result.getOrNull()
        assertNotNull(restored)
        assertEquals(5, restored?.size)
        assertEquals(original[0].name, restored?.get(0)?.name)
        assertEquals(original[0].sat, restored?.get(0)?.sat)
        assertEquals(original[0].con, restored?.get(0)?.con)
    }

    @Test
    fun testYamlExportAndParse() {
        val portra = RecipeCatalog.getById("kodak_portra_400")!!
        val yaml = RecipeFormatV1Parser.exportToYaml(portra)
        assertTrue(yaml.contains("format: 1"))
        assertTrue(yaml.contains("Kodak Portra 400"))

        val parsed = RecipeFormatV1Parser.parseYaml(yaml)
        assertTrue(parsed.isSuccess)
        val restored = parsed.getOrNull()
        assertNotNull(restored)
        assertEquals(portra.name, restored?.name)
        assertEquals(portra.sat, restored?.sat)
        assertEquals(portra.con, restored?.con)
        assertEquals(portra.ab, restored?.ab)
        assertEquals(portra.gm, restored?.gm)
    }
}
