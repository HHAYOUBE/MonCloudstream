package com.hhayoube.animesama

import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.nicehttp.RequestBodyTypes
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AnimeSamaProvider : MainAPI() {
    override var mainUrl = AnimeSamaConfig.baseUrl
    override var name = "AnimeSama"
    override var lang = "fr"
    override val supportedTypes = setOf(TvType.Anime)
    private val endpoint = "https://graphql.anilist.co"

    override val hasMainPage = true
    override val mainPage = mainPageOf(
        "TRENDING_DESC" to "Tendances",
        "POPULARITY_DESC" to "Les plus populaires",
        "SCORE_DESC" to "Les mieux notés",
        "START_DATE_DESC" to "Nouveautés"
    )

    override suspend fun getMainPage(page: Int, pageRequest: MainPageRequest): HomePageResponse {
        if (page < 1) return newHomePageResponse(pageRequest.name, emptyList())
        val gql = """
            query (${'$'}page: Int, ${'$'}sort: [MediaSort]) {
              Page(page: ${'$'}page, perPage: 20) {
                media(sort: ${'$'}sort, type: ANIME, isAdult: false) {
                  id title { romaji english native } coverImage { large extraLarge }
                  startDate { year } episodes
                }
              }
            }
        """.trimIndent()
        val root = try {
            request(gql, mapOf("page" to page, "sort" to listOf(pageRequest.data)))
        } catch (_: Exception) {
            return newHomePageResponse(pageRequest.name, emptyList())
        }
        val media = root.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media")
            ?: return newHomePageResponse(pageRequest.name, emptyList())
        val results = (0 until media.length()).mapNotNull { i ->
            val item = media.optJSONObject(i) ?: return@mapNotNull null
            val titles = item.optJSONObject("title") ?: JSONObject()
            val title = titles.optString("english").takeIf { it.isNotBlank() && it != "null" }
                ?: titles.optString("romaji").takeIf { it.isNotBlank() && it != "null" }
                ?: titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
                ?: return@mapNotNull null
            val id = item.optInt("id", 0)
            if (id <= 0) return@mapNotNull null
            val cover = item.optJSONObject("coverImage")
            val poster = cover?.optString("extraLarge")?.takeIf { it.isNotBlank() && it != "null" }
                ?: cover?.optString("large")?.takeIf { it.isNotBlank() && it != "null" }
            newAnimeSearchResponse(title, "$mainUrl/anilist/$id", TvType.Anime) {
                posterUrl = poster
                year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
                item.optInt("episodes", 0).takeIf { it > 0 }?.let { episodes[DubStatus.Subbed] = it }
                otherName = titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
            }
        }
        return newHomePageResponse(pageRequest, results, hasNext = media.length() == 20)
    }

    private suspend fun request(query: String, variables: Map<String, Any?>): JSONObject {
        val body = mapOf("query" to query, "variables" to variables).toJson()
            .toRequestBody(RequestBodyTypes.JSON.toMediaTypeOrNull())
        val response = JSONObject(app.post(endpoint, requestBody = body, headers = mapOf("Accept" to "application/json")).text)
        if (response.has("errors")) throw IllegalStateException("AniList GraphQL returned errors")
        return response
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        val gql = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 20) {
                media(search: ${'$'}search, type: ANIME, isAdult: false, sort: SEARCH_MATCH) {
                  id title { romaji english native } coverImage { large extraLarge }
                  startDate { year } episodes
                }
              }
            }
        """.trimIndent()
        val root = try { request(gql, mapOf("search" to term)) } catch (_: Exception) { return emptyList() }
        val media = root.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media") ?: return emptyList()
        return (0 until media.length()).mapNotNull { i ->
            val item = media.optJSONObject(i) ?: return@mapNotNull null
            val titles = item.optJSONObject("title") ?: JSONObject()
            val title = titles.optString("english").takeIf { it.isNotBlank() && it != "null" }
                ?: titles.optString("romaji").takeIf { it.isNotBlank() && it != "null" }
                ?: titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
                ?: return@mapNotNull null
            val id = item.optInt("id", 0)
            if (id <= 0) return@mapNotNull null
            val cover = item.optJSONObject("coverImage")
            val poster = cover?.optString("extraLarge")?.takeIf { it.isNotBlank() && it != "null" }
                ?: cover?.optString("large")?.takeIf { it.isNotBlank() && it != "null" }
            newAnimeSearchResponse(title, "$mainUrl/anilist/$id", TvType.Anime) {
                posterUrl = poster
                year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
                item.optInt("episodes", 0).takeIf { it > 0 }?.let { episodes[DubStatus.Subbed] = it }
                otherName = titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = Regex("/anilist/(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        val gql = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id title { romaji english native } description(asHtml: false)
                coverImage { large extraLarge } bannerImage startDate { year }
                episodes duration genres synonyms streamingEpisodes { title thumbnail url site }
              }
            }
        """.trimIndent()
        val root = try { request(gql, mapOf("id" to id)) } catch (_: Exception) { return null }
        val item = root.optJSONObject("data")?.optJSONObject("Media") ?: return null
        val titles = item.optJSONObject("title") ?: JSONObject()
        val title = titles.optString("english").takeIf { it.isNotBlank() && it != "null" }
            ?: titles.optString("romaji").takeIf { it.isNotBlank() && it != "null" }
            ?: titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
            ?: return null
        val cover = item.optJSONObject("coverImage")
        val poster = cover?.optString("extraLarge")?.takeIf { it.isNotBlank() && it != "null" }
            ?: cover?.optString("large")?.takeIf { it.isNotBlank() && it != "null" }
        val officialVideoEpisodes = item.optJSONArray("streamingEpisodes")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val stream = array.optJSONObject(index) ?: return@mapNotNull null
                val streamUrl = stream.optString("url").takeIf { it.isNotBlank() && it != "null" }
                    ?: return@mapNotNull null
                val streamUri = try { java.net.URI(streamUrl) } catch (_: Exception) { return@mapNotNull null }
                val host = streamUri.host?.lowercase() ?: return@mapNotNull null
                val isYouTube = host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
                if (streamUri.scheme != "https" || !isYouTube) return@mapNotNull null
                val streamTitle = stream.optString("title").takeIf { it.isNotBlank() && it != "null" }
                    ?: "Vidéo YouTube ${index + 1}"
                streamTitle to streamUrl
            }
        } ?: emptyList()
        val manualEpisodes = ManualEpisodeLinks.links[id].orEmpty()
        val episodeSources = (officialVideoEpisodes + manualEpisodes.mapIndexed { index, episodeUrl ->
            "Épisode manuel ${index + 1}" to episodeUrl
        }).distinctBy { it.second }
        return newAnimeLoadResponse(title, "$mainUrl/anilist/$id", TvType.Anime) {
            engName = titles.optString("english").takeIf { it.isNotBlank() && it != "null" }
            japName = titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
            posterUrl = poster
            backgroundPosterUrl = item.optString("bannerImage").takeIf { it.isNotBlank() && it != "null" }
            year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
            plot = item.optString("description").takeIf { it.isNotBlank() && it != "null" }
            duration = item.optInt("duration", 0).takeIf { it > 0 }
            tags = item.optJSONArray("genres")?.let { arr -> (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) } }
            synonyms = item.optJSONArray("synonyms")?.let { arr -> (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) } }
            episodes[DubStatus.Subbed] = episodeSources.mapIndexed { index, (episodeTitle, episodeUrl) ->
                newEpisode(episodeUrl) {
                    name = episodeTitle
                    episode = index + 1
                }
            }
            comingSoon = false
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        // Accept only direct media URLs from a source the user is authorized to access.
        // A normal webpage URL is not a playable video stream and must not be advertised as one.
        val uri = try { java.net.URI(data) } catch (_: Exception) { return false }
        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) return false

        val host = uri.host.lowercase()
        val isYouTube = host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
        if (isYouTube) {
            // AniList-listed YouTube pages are passed to CloudStream's supported extractor.
            // No DRM, login, or paywall bypass is attempted.
            return loadExtractor(data, mainUrl, subtitleCallback, callback)
        }

        val path = uri.path.orEmpty().lowercase()
        val type = when {
            path.endsWith(".m3u8") -> ExtractorLinkType.M3U8
            path.endsWith(".mpd") -> ExtractorLinkType.DASH
            path.endsWith(".mp4") || path.endsWith(".m4v") ||
                path.endsWith(".webm") || path.endsWith(".mkv") -> ExtractorLinkType.VIDEO
            else -> return false
        }

        callback(newExtractorLink(source = name, name = "Vidéo directe", url = data, type = type))
        return true
    }
}
