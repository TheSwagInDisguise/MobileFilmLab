package com.mobilefilmlab.camera.updates

import com.google.gson.JsonParser
import com.mobilefilmlab.camera.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Explicit user-initiated check only. No silent downloads, installation, telemetry or credentials. */
object ReleaseChecker {
    const val RELEASES="https://github.com/TheSwagInDisguise/MobileFilmLab/releases"
    data class Release(val tag: String,val page: String,val apk: String,val notes: String)
    suspend fun check(): Result<Release?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection=URL("https://api.github.com/repos/TheSwagInDisguise/MobileFilmLab/releases?per_page=30").openConnection() as HttpURLConnection
            connection.connectTimeout=12000; connection.readTimeout=12000
            connection.setRequestProperty("Accept","application/vnd.github+json")
            connection.setRequestProperty("User-Agent","MobileFilmLab/${BuildConfig.VERSION_NAME}")
            val json=try {
                check(connection.responseCode==200) { "Update check unavailable (HTTP ${connection.responseCode})" }
                connection.inputStream.use { input ->
                    val buffer=ByteArray(8192); val output=java.io.ByteArrayOutputStream()
                    while(true) { val n=input.read(buffer); if(n<0) break; check(output.size()+n<=1_048_576) { "Release response too large" }; output.write(buffer,0,n) }
                    output.toString("UTF-8")
                }
            } finally { connection.disconnect() }
            val current=ReleaseVersion.parse(BuildConfig.VERSION_NAME) ?: error("Invalid installed version")
            JsonParser.parseString(json).asJsonArray.mapNotNull { item ->
                val r=item.asJsonObject
                if(r["draft"].asBoolean) return@mapNotNull null
                val tag=r["tag_name"].asString; val v=ReleaseVersion.parse(tag) ?: return@mapNotNull null
                // Alpha testing channel: beta/RC are intentionally not offered before explicit promotion.
                if(v<=current || v.stage in 1..2) return@mapNotNull null
                val asset=r.getAsJsonArray("assets").firstOrNull {
                    it.asJsonObject["name"].asString=="MobileFilmLab-$tag-signed.apk"
                }?.asJsonObject ?: return@mapNotNull null
                val page=r["html_url"].asString; val apk=asset["browser_download_url"].asString
                if(!page.startsWith("$RELEASES/tag/") || !apk.startsWith("$RELEASES/download/")) return@mapNotNull null
                v to Release(tag,page,apk,r["body"]?.takeUnless { it.isJsonNull }?.asString.orEmpty().take(3000))
            }.maxByOrNull { it.first }?.second
        }
    }
}
