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
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * General-purpose metadata catalogue backed by Cinemeta.
 *
 * Cinemeta supplies public metadata and artwork, not playable video streams.
 * This provider deliberately does not invent episode URLs or bypass platform
 * access controls. Playback will only be added when an official/authorized
 * source with a supported resolver is integrated and tested.
 */
class GeneralCatalogProvider : MainAPI() {
    override var mainUrl = "https://v3-cinemeta.strem.io"
    override var name = "SenpaiStream — Catalogue général"
    override var lang = "fr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "movie:top" to "Films populaires",
        "series:top" to "Séries populaires",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (page < 1) return newHomePageResponse(request.name, emptyList())
        val type = request.data.substringBefore(':')
        if (type != "movie" && type != "series") {
            return newHomePageResponse(request.name, emptyList())
        }
        val items = fetchCatalog(type, page, null)
        return newHomePageResponse(request, items, hasNext = items.size >= PAGE_SIZE)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        val movies = runCatching { fetchCatalog("movie", 1, term) }.getOrDefault(emptyList())
        val series = runCatching { fetchCatalog("series", 1, term) }.getOrDefault(emptyList())
        return (movies + series).distinctBy { it.url }.take(MAX_SEARCH_RESULTS)
    }

    private suspend fun fetchCatalog(
        type: String,
        page: Int,
        search: String?,
    ): List<SearchResponse> {
        val path = if (search.isNullOrBlank()) {
            "/catalog/$type/top.json?skip=${(page - 1) * PAGE_SIZE}"
        } else {
            "/catalog/$type/top/search=${encode(search)}.json"
        }
        val root = requestJson(mainUrl + path)
        val metas = root.optJSONArray("metas") ?: JSONArray()
        return (0 until metas.length()).mapNotNull { index ->
            val item = metas.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val title = item.optString("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val itemType = item.optString("type").takeIf { it == "movie" || it == "series" } ?: type
            val itemUrl = "$mainUrl/meta/$itemType/$id"
            val poster = item.optString("poster").takeIf(String::isNotBlank)
            val year = yearFrom(item.optString("releaseInfo"))
            if (itemType == "movie") {
                newMovieSearchResponse(title, itemUrl, TvType.Movie) {
                    posterUrl = poster
                    this.year = year
                }
            } else {
                newTvSeriesSearchResponse(title, itemUrl, TvType.TvSeries) {
                    posterUrl = poster
                    this.year = year
                }
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val match = Regex("/meta/(movie|series)/([^/?#]+)").find(url) ?: return null
        val type = match.groupValues[1]
        val id = match.groupValues[2]
        val root = runCatching { requestJson("$mainUrl/meta/$type/$id.json") }.getOrNull() ?: return null
        val item = root.optJSONObject("meta") ?: return null
        val title = item.optString("name").takeIf(String::isNotBlank) ?: return null
        val poster = item.optString("poster").takeIf(String::isNotBlank)
        val background = item.optString("background").takeIf(String::isNotBlank)
        val description = item.optString("description").takeIf(String::isNotBlank)
        val year = yearFrom(item.optString("releaseInfo"))
        val genres = item.optJSONArray("genres")?.asStringList().orEmpty()
        val imdbRating = item.optString("imdbRating").toDoubleOrNull()

        if (type == "movie") {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                posterUrl = poster
                backgroundPosterUrl = background
                this.year = year
                plot = description
                tags = genres
                comingSoon = true
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, emptyList()) {
            posterUrl = poster
            backgroundPosterUrl = background
            this.year = year
            plot = description
            tags = genres
            comingSoon = true
        }
    }

    private suspend fun requestJson(url: String): JSONObject {
        val response = app.get(
            url = url,
            headers = mapOf("Accept" to "application/json"),
            timeout = 15,
            cacheTime = 300,
        )
        if (response.okhttpResponse.code !in 200..299) {
            throw ErrorLoadingException("Catalogue général indisponible (HTTP ${response.okhttpResponse.code})")
        }
        return JSONObject(response.text)
    }

    private fun yearFrom(value: String): Int? =
        Regex("(?:19|20)\\d{2}").find(value)?.value?.toIntOrNull()

    private fun JSONArray.asStringList(): List<String> =
        (0 until length()).mapNotNull { index ->
            optString(index).takeIf { it.isNotBlank() && it != "null" }
        }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        // Cinemeta is a metadata source, not a video host. Never treat its
        // metadata IDs as stream URLs.
        return false
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_SEARCH_RESULTS = 40
    }
}
