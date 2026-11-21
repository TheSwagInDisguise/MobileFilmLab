package com.mobilefilmlab.camera

import com.google.gson.JsonParser
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import com.mobilefilmlab.camera.data.importer.RecipeJsonSerializer
import com.mobilefilmlab.camera.data.importer.RecipeQrCodec
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Contract tests add no Android production changes. Same fixtures are consumed by native Swift tests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SharedIOSContractTest {
    private fun fixture(name:String):String = listOf(File("../shared/$name"),File("shared/$name")).first { it.isFile }.readText()
    @Test fun all160SRGBMatricesMatchSharedIndependentVectors() {
        val vectors=JsonParser.parseString(fixture("colour-vectors.json")).asJsonArray
        assertEquals(160,vectors.size())
        vectors.forEach { element ->
            val v=element.asJsonObject
            val recipe=RecipeCatalog.ALL_RECIPES.first { it.id==v["id"].asString }.copy(puckTone=v["tone"].asFloat,puckColor=v["color"].asFloat)
            val actual=ImageProcessor.buildRecipeColorMatrix(recipe).array
            v["matrix"].asJsonArray.forEachIndexed { i, expected -> assertEquals("${recipe.id} coefficient $i",expected.asFloat,actual[i],0.0001f) }
        }
    }
    @Test fun fullCatalogueDataMatchesAndroidFieldForField() {
        val root=JsonParser.parseString(fixture("catalogue.json")).asJsonObject
        assertEquals(80,root["recipes"].asJsonArray.size())
        val envelope="{\"type\":\"mobilefilmlab.recipe\",\"version\":1,\"recipes\":${root["recipes"]}}"
        assertEquals(RecipeCatalog.ALL_RECIPES,RecipeJsonSerializer.fromJson(envelope).getOrThrow())
    }
    @Test fun javaDeflaterAndVersionedJSONFixturesDecodeIdenticallyToSwift() {
        val fixture=JsonParser.parseString(fixture("interchange-fixtures.json")).asJsonObject
        val json=RecipeJsonSerializer.fromJson(fixture["document"].toString()).getOrThrow()
        val qr=RecipeQrCodec.decodePayload(fixture["androidDeflaterQR"].asString).getOrThrow()
        assertEquals(json,qr);assertEquals(listOf(RecipeCatalog.ALL_RECIPES[3]),qr)
    }
}
