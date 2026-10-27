package com.mobilefilmlab.camera.updates

import com.google.gson.JsonParser
import com.mobilefilmlab.camera.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Explicit user-initiated check only. No silent downloads, installation, telemetry or credentials. */
object ReleaseChecker {
    const val RELEASES = "https://github.com/TheSwagInDisguise/MobileFilmLab/releases"
    data class Release(val tag: String, val page: String, val apk: String, val notes: String)

    val channelLabel: String
        get() = when (ReleaseVersion.parse(BuildConfig.VERSION_NAME)?.stage) {
            0 -> "alpha / stable"
            1 -> "beta / stable"
            2 -> "RC / stable"
            else -> "stable"
        }

    /** Stay on the installed testing channel, or offer a newer stable release. */
    internal fun eligible(current: ReleaseVersion, candidate: ReleaseVersion): Boolean =
        candidate > current && (candidate.stage == current.stage || candidate.stage == 3)

    /** Kept separate from networking so channel selection and release identity are regression-tested. */
    internal fun selectRelease(json: String, installedVersion: String): Release? {
        val current = ReleaseVersion.parse(installedVersion) ?: error("Invalid installed version")
        return JsonParser.parseString(json).asJsonArray.mapNotNull { item ->
            // A malformed entry should not hide otherwise valid releases.
            runCatching {
                val r = item.asJsonObject
                if (r["draft"].asBoolean) return@runCatching null
                val tag = r["tag_name"].asString
                val version = ReleaseVersion.parse(tag) ?: return@runCatching null
                if (!eligible(current, version)) return@runCatching null
                val name = "MobileFilmLab-$tag-signed.apk"
                val asset = r.getAsJsonArray("assets").firstOrNull {
                    it.asJsonObject["name"].asString == name
                }?.asJsonObject ?: return@runCatching null
                val page = r["html_url"].asString
                val apk = asset["browser_download_url"].asString
                if (page != "$RELEASES/tag/$tag" || apk != "$RELEASES/download/$tag/$name") return@runCatching null
                version to Release(tag, page, apk, r["body"]?.takeUnless { it.isJsonNull }?.asString.orEmpty().take(3000))
            }.getOrNull()
        }.maxByOrNull { it.first }?.second
    }

    suspend fun check(): Result<Release?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://api.github.com/repos/TheSwagInDisguise/MobileFilmLab/releases?per_page=30")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "MobileFilmLab/${BuildConfig.VERSION_NAME}")
            val json = try {
                check(connection.responseCode == 200) { "Update check unavailable (HTTP ${connection.responseCode})" }
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    val output = java.io.ByteArrayOutputStream()
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        check(output.size() + n <= 1_048_576) { "Release response too large" }
                        output.write(buffer, 0, n)
                    }
                    output.toString("UTF-8")
                }
            } finally { connection.disconnect() }
            selectRelease(json, BuildConfig.VERSION_NAME)
        }
    }
}
