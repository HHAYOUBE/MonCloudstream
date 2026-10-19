package com.hhayoube.senpaistream

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** Catalogue Internet Archive limité aux films explicitement marqués comme domaine public. */
class PublicDomainFilmsProvider : MainAPI() {
    override var mainUrl = "https://archive.org"
    override var name = "Films du domaine public"
    override var lang = "fr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "popular" to "Films populaires du domaine public",
        "recent" to "Films récemment ajoutés"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (page < 1) return newHomePageResponse(request.name, emptyList())
        val sort = if (request.data == "recent") "publicdate desc" else "downloads desc"
        val items = searchArchive("*", page, sort)
        return newHomePageResponse(request, items, hasNext = items.size >= PAGE_SIZE)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        return searchArchive(term, 1, "downloads desc")
    }

    private suspend fun searchArchive(term: String, page: Int, sort: String): List<SearchResponse> {
        val safeTerm = term.replace("(", " ").replace(")", " ")
        val query = if (term == "*") {
            "mediatype:movies AND licenseurl:\"$PUBLIC_DOMAIN_MARK\""
        } else {
            "mediatype:movies AND licenseurl:\"$PUBLIC_DOMAIN_MARK\" AND title:($safeTerm)"
        }
        val fields = listOf("identifier", "title", "year", "description", "licenseurl")
            .joinToString("") { "&fl[]=" + encode(it) }
        val url = "$mainUrl/advancedsearch.php?q=${encode(query)}$fields&output=json&rows=$PAGE_SIZE&page=$page&sort[]=${encode(sort)}"
        val root = requestJson(url)
        val response = root.optJSONObject("response")
            ?: throw ErrorLoadingException("Internet Archive : réponse de recherche invalide")
        val docs = response.optJSONArray("docs") ?: JSONArray()
        return (0 until docs.length()).mapNotNull { index ->
            val item = docs.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("identifier").takeIf { it.isNotBlank() && it != "null" }
                ?: return@mapNotNull null
            if (!isPublicDomain(item.optString("licenseurl"))) return@mapNotNull null
            val title = item.optString("title").takeIf { it.isNotBlank() && it != "null" } ?: id
            newMovieSearchResponse(title, "$mainUrl/details/$id", TvType.Movie) {
                posterUrl = "$mainUrl/services/img/$id"
                year = item.optString("year").toIntOrNull()
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = url.substringAfter("/details/", "").substringBefore('?').trim()
            .takeIf { it.matches(Regex("[A-Za-z0-9._-]+")) } ?: return null
        val item = fetchItem(id) ?: return null
        val metadata = item.optJSONObject("metadata") ?: return null
        if (!isPublicDomain(metadata.optString("licenseurl"))) {
            throw ErrorLoadingException("Ce film n'est pas identifié comme appartenant au domaine public.")
        }
        val title = metadata.optString("title").takeIf { it.isNotBlank() && it != "null" } ?: id
        val description = metadata.optString("description").takeIf { it.isNotBlank() && it != "null" }
        val year = metadata.optString("year").toIntOrNull()
        val files = item.optJSONArray("files") ?: JSONArray()
        val playable = playableFiles(files)
        return newMovieLoadResponse(title, "$mainUrl/details/$id", TvType.Movie, id) {
            posterUrl = "$mainUrl/services/img/$id"
            plot = description
            this.year = year
            comingSoon = playable.isEmpty()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val id = data.trim().takeIf { it.matches(Regex("[A-Za-z0-9._-]+")) } ?: return false
        val item = try { fetchItem(id) } catch (_: Exception) { return false } ?: return false
        val metadata = item.optJSONObject("metadata") ?: return false
        if (!isPublicDomain(metadata.optString("licenseurl"))) return false

        var found = false
        val files = item.optJSONArray("files") ?: JSONArray()
        for (index in 0 until files.length()) {
            val file = files.optJSONObject(index) ?: continue
            val filename = file.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val encodedName = encode(filename).replace("+", "%20")
            val fileUrl = "$mainUrl/download/$id/$encodedName"
            when {
                filename.endsWith(".srt", true) || filename.endsWith(".vtt", true) -> {
                    val language = when {
                        Regex("(?i)(^|[._ -])fr([._ -]|$)|french|français").containsMatchIn(filename) -> "Français"
                        Regex("(?i)(^|[._ -])en([._ -]|$)|english").containsMatchIn(filename) -> "English"
                        else -> "Sous-titres"
                    }
                    subtitleCallback(SubtitleFile(language, fileUrl))
                }
                isPlayableVideo(filename) -> {
                    callback(newExtractorLink(
                        source = name,
                        name = filename,
                        url = fileUrl,
                        type = ExtractorLinkType.VIDEO
                    ))
                    found = true
                }
            }
        }
        return found
    }

    private suspend fun fetchItem(id: String): JSONObject? {
        val response = app.get("$mainUrl/metadata/$id", timeout = 20, cacheTime = 300)
        if (response.okhttpResponse.code !in 200..299) {
            throw ErrorLoadingException("Internet Archive a répondu HTTP ${response.okhttpResponse.code}")
        }
        return JSONObject(response.text)
    }

    private suspend fun requestJson(url: String): JSONObject {
        val response = app.get(url, headers = mapOf("Accept" to "application/json"), timeout = 20, cacheTime = 300)
        if (response.okhttpResponse.code !in 200..299) {
            throw ErrorLoadingException("Recherche Internet Archive impossible (HTTP ${response.okhttpResponse.code})")
        }
        return JSONObject(response.text)
    }

    private fun playableFiles(files: JSONArray): List<String> =
        (0 until files.length()).mapNotNull { index ->
            files.optJSONObject(index)?.optString("name")
                ?.takeIf { it.isNotBlank() && isPlayableVideo(it) }
        }

    private fun isPlayableVideo(filename: String): Boolean =
        listOf(".mp4", ".m4v", ".webm", ".ogv").any { filename.endsWith(it, true) }

    private fun isPublicDomain(license: String): Boolean =
        license.trim().trimEnd('/') == PUBLIC_DOMAIN_MARK.trimEnd('/')

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val PAGE_SIZE = 20
        const val PUBLIC_DOMAIN_MARK = "https://creativecommons.org/publicdomain/mark/1.0/"
    }
}
