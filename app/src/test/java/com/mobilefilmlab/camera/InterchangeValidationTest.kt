package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.data.importer.RecipeJsonSerializer as S
import com.mobilefilmlab.camera.data.importer.RecipeQrCodec as Q
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import com.mobilefilmlab.camera.updates.ReleaseVersion as V
import org.junit.Assert.*
import org.junit.Test

class InterchangeValidationTest {
    private val recipe=RecipeCatalog.getDefaultRecipe()
    @Test fun all80RecipesRoundTripWithoutLosingTuning() { assertEquals(RecipeCatalog.ALL_RECIPES,S.fromJson(S.toJson(RecipeCatalog.ALL_RECIPES)).getOrThrow()) }
    @Test fun nullOrEmptyOrMalformedDocumentsAreRejected() {
        for(json in listOf("null","[]","{}","[null]","not json","[{\"name\":null}]")) assertTrue(S.fromJson(json).isFailure)
    }
    @Test fun untrustedTuningBoundsAreEnforced() {
        val json=S.toJson(listOf(recipe)).replace("\"sat\": 0","\"sat\": 999")
        assertTrue(S.fromJson(json).isFailure)
    }
    @Test fun unrecognizedEnumAndVersionAreRejected() {
        assertTrue(S.fromJson(S.toJson(listOf(recipe)).replace("\"OPPO\"","\"NOT_A_GROUP\"")).isFailure)
        assertTrue(S.fromJson(S.toJson(listOf(recipe)).replace("\"version\": 1","\"version\": 999")).isFailure)
    }
    @Test fun fileSizeLimitIsEnforced() { assertTrue(S.fromJson(" ".repeat(S.MAX_BYTES+1)).isFailure) }
    @Test fun qrPayloadRoundTripIncludesPuckValues() {
        val tuned=recipe.copy(puckTone=33f,puckColor=-44f)
        assertEquals(listOf(tuned),Q.decodePayload(Q.payload(tuned)).getOrThrow())
    }
    @Test fun malformedQrDoesNotCrash() { assertTrue(Q.decodePayload("FILMRECIPE:1:not-valid").isFailure); assertTrue(Q.decodePayload("https://example.org").isFailure) }
    @Test fun numericPrereleaseComparisonIsSemantic() {
        assertTrue(V.parse("v0.1.0-alpha.10")!!>V.parse("v0.1.0-alpha.9")!!)
        assertTrue(V.parse("0.1.0")!!>V.parse("0.1.0-beta.99")!!)
        assertNull(V.parse("unsafe/latest"))
    }
}
