package com.mobilefilmlab.camera.updates

import org.junit.Assert.*
import org.junit.Test

class ReleaseCheckerTest {
    private fun version(value: String) = ReleaseVersion.parse(value)!!
    private fun release(tag: String, draft: Boolean = false, apk: String? = null, page: String? = null, name: String? = null): String {
        val filename = name ?: "MobileFilmLab-$tag-signed.apk"
        return """{"tag_name":"$tag","draft":$draft,"html_url":"${page ?: "${ReleaseChecker.RELEASES}/tag/$tag"}","body":"Test notes","assets":[{"name":"$filename","browser_download_url":"${apk ?: "${ReleaseChecker.RELEASES}/download/$tag/$filename"}"}]}"""
    }
    private fun select(vararg entries: String, current: String = "0.2.0-beta.1") =
        ReleaseChecker.selectRelease(entries.joinToString(",", "[", "]"), current)

    @Test fun betaOffersNewerBetaAndStableButNotAlphaOrRc() {
        val current = version("0.2.0-beta.1")
        assertTrue(ReleaseChecker.eligible(current, version("0.2.0-beta.2")))
        assertTrue(ReleaseChecker.eligible(current, version("0.2.0")))
        assertFalse(ReleaseChecker.eligible(current, version("0.3.0-alpha.1")))
        assertFalse(ReleaseChecker.eligible(current, version("0.2.0-rc.1")))
    }
    @Test fun stableDoesNotOfferAnyPrerelease() {
        val current = version("0.2.0")
        assertFalse(ReleaseChecker.eligible(current, version("0.3.0-beta.1")))
        assertTrue(ReleaseChecker.eligible(current, version("0.2.1")))
    }
    @Test fun sameVersionAndDowngradesAreNotOffered() {
        assertNull(select(release("v0.2.0-beta.1"), release("v0.1.0-alpha.9"), release("v0.1.0")))
    }
    @Test fun highestSemanticVersionWinsNotLexicalOrResponseOrder() {
        assertEquals("v0.2.0-beta.10", select(release("v0.2.0-beta.2"), release("v0.2.0-beta.10"), release("v0.2.0-beta.9"))?.tag)
    }
    @Test fun draftAndWrongAssetAreIgnored() {
        assertNull(select(release("v0.2.0-beta.2", draft = true), release("v0.2.0-beta.3", name = "unsigned.apk")))
    }
    @Test fun externalOrMismatchedReleaseLinksAreRejected() {
        assertNull(select(release("v0.2.0-beta.2", apk = "https://example.com/app.apk")))
        assertNull(select(release("v0.2.0-beta.2", page = "${ReleaseChecker.RELEASES}/tag/v0.2.0-beta.9")))
        assertNull(select(release("v0.2.0-beta.2", apk = "${ReleaseChecker.RELEASES}/download/v0.2.0-beta.9/MobileFilmLab-v0.2.0-beta.2-signed.apk")))
    }
    @Test fun malformedEntryDoesNotHideValidRelease() {
        assertEquals("v0.2.0-beta.2", select("{}", release("not-a-version"), release("v0.2.0-beta.2"))?.tag)
    }
    @Test fun alphaChannelStillWorksWithoutImplicitPromotion() {
        assertEquals("v0.1.0-alpha.10", select(release("v0.2.0-beta.1"), release("v0.1.0-alpha.10"), current = "0.1.0-alpha.9")?.tag)
    }
    @Test fun currentBuildExposesBetaChannelLabel() {
        assertEquals("beta / stable", ReleaseChecker.channelLabel)
    }
}
