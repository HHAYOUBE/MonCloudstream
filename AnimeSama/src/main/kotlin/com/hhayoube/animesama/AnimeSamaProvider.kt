package com.hhayoube.animesama

import com.lagradost.cloudstream3.ErrorLoadingException
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
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
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
    override val supportedTypes = setOf(TvType.Anime, TvType.Movie)
    private val endpoint = "https://graphql.anilist.co"

    override val hasMainPage = true
    override val mainPage = mainPageOf(
        "TRENDING_DESC" to "Tendances",
        "POPULARITY_DESC" to "Les plus populaires",
        "SCORE_DESC" to "Les mieux notés",
        "START_DATE_DESC" to "Nouveautés",
        "FAVOURITES_DESC" to "Les plus aimés",
        "END_DATE_DESC" to "Séries terminées récemment",
        "EPISODES_DESC" to "Séries longues",
        "TITLE_ROMAJI" to "Ordre alphabétique"
    )

    override suspend fun getMainPage(page: Int, pageRequest: MainPageRequest): HomePageResponse {
        if (page < 1) return newHomePageResponse(pageRequest.name, emptyList())
        val gql = """
            query (${'$'}page: Int, ${'$'}sort: [MediaSort]) {
              Page(page: ${'$'}page, perPage: 20) {
                media(sort: ${'$'}sort, type: ANIME, isAdult: false) {
                  id title { romaji english native } coverImage { large extraLarge }
                  startDate { year } episodes format
                }
              }
            }
        """.trimIndent()
        val root = try {
            request(gql, mapOf("page" to page, "sort" to listOf(pageRequest.data)))
        } catch (e: Exception) {
            throw ErrorLoadingException("Catalogue AniList indisponible : " + (e.message ?: "erreur réseau"))
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
            if (item.optString("format") == "MOVIE") {
                newMovieSearchResponse(title, "$mainUrl/anilist/$id", TvType.Movie) {
                    posterUrl = poster
                    year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
                }
            } else {
                newAnimeSearchResponse(title, "$mainUrl/anilist/$id", TvType.Anime) {
                    posterUrl = poster
                    year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
                    item.optInt("episodes", 0).takeIf { it > 0 }?.let { episodes[DubStatus.Subbed] = it }
                    otherName = titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
                }
            }
        }
        return newHomePageResponse(pageRequest, results, hasNext = media.length() == 20)
    }

    private suspend fun request(query: String, variables: Map<String, Any?>): JSONObject {
        val body = mapOf("query" to query, "variables" to variables).toJson()
            .toRequestBody(RequestBodyTypes.JSON.toMediaTypeOrNull())
        val httpResponse = app.post(endpoint, requestBody = body, headers = mapOf("Accept" to "application/json"))
        if (httpResponse.okhttpResponse.code !in 200..299) {
            throw ErrorLoadingException("AniList a répondu HTTP " + httpResponse.okhttpResponse.code)
        }
        val response = JSONObject(httpResponse.text)
        if (response.has("errors")) {
            val details = response.optJSONArray("errors")?.optJSONObject(0)?.optString("message")
            throw ErrorLoadingException("AniList GraphQL : " + (details?.takeIf { it.isNotBlank() } ?: "erreur inconnue"))
        }
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
                  startDate { year } episodes format
                }
              }
            }
        """.trimIndent()
        val root = try { request(gql, mapOf("search" to term)) } catch (e: Exception) { throw ErrorLoadingException("Recherche AniList impossible : " + (e.message ?: "erreur réseau")) }
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
            if (item.optString("format") == "MOVIE") {
                newMovieSearchResponse(title, "$mainUrl/anilist/$id", TvType.Movie) {
                    posterUrl = poster
                    year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
                }
            } else {
                newAnimeSearchResponse(title, "$mainUrl/anilist/$id", TvType.Anime) {
                    posterUrl = poster
                    year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
                    item.optInt("episodes", 0).takeIf { it > 0 }?.let { episodes[DubStatus.Subbed] = it }
                    otherName = titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
                }
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
                episodes duration genres synonyms format streamingEpisodes { title thumbnail url site }
              }
            }
        """.trimIndent()
        val root = try {
            request(gql, mapOf("id" to id))
        } catch (e: Exception) {
            throw ErrorLoadingException("Fiche AniList indisponible : " + (e.message ?: "erreur réseau"))
        }
        val item = root.optJSONObject("data")?.optJSONObject("Media")
            ?: throw ErrorLoadingException("Fiche anime introuvable dans AniList (ID $id).")
        val titles = item.optJSONObject("title") ?: JSONObject()
        val title = titles.optString("english").takeIf { it.isNotBlank() && it != "null" }
            ?: titles.optString("romaji").takeIf { it.isNotBlank() && it != "null" }
            ?: titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
            ?: return null
        val cover = item.optJSONObject("coverImage")
        val poster = cover?.optString("extraLarge")?.takeIf { it.isNotBlank() && it != "null" }
            ?: cover?.optString("large")?.takeIf { it.isNotBlank() && it != "null" }
        // AniList can provide episode pages from licensed streaming platforms.
        // Keep only HTTPS links on known official providers; availability may vary by region/account.
        val officialVideoEpisodes = item.optJSONArray("streamingEpisodes")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val stream = array.optJSONObject(index) ?: return@mapNotNull null
                val streamUrl = stream.optString("url").takeIf { it.isNotBlank() && it != "null" }
                    ?: return@mapNotNull null
                val streamUri = try { java.net.URI(streamUrl) } catch (_: Exception) { return@mapNotNull null }
                val host = streamUri.host?.lowercase() ?: return@mapNotNull null
                if (streamUri.scheme != "https" || !isOfficialStreamingHost(host)) return@mapNotNull null
                val streamTitle = stream.optString("title").takeIf { it.isNotBlank() && it != "null" }
                    ?: "Épisode ${index + 1}"
                val site = stream.optString("site").takeIf { it.isNotBlank() && it != "null" }
                (if (site != null) "$site — $streamTitle" else streamTitle) to streamUrl
            }
        } ?: emptyList()
        val manualEpisodes = ManualEpisodeLinks.links[id].orEmpty()
            .filter { it.episode > 0 && it.source.isNotBlank() && it.url.isNotBlank() }
            .filter { it.language.equals("VF", ignoreCase = true) || it.language.equals("VOSTFR", ignoreCase = true) }
        if (item.optString("format") == "MOVIE") {
            val movieData = mapOf(
                "sources" to officialVideoEpisodes.map { (sourceTitle, sourceUrl) ->
                    mapOf("name" to sourceTitle, "url" to sourceUrl)
                }
            ).toJson()
            return newMovieLoadResponse(title, "$mainUrl/anilist/$id", TvType.Movie, movieData) {
                posterUrl = poster
                backgroundPosterUrl = item.optString("bannerImage").takeIf { it.isNotBlank() && it != "null" }
                year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
                plot = item.optString("description").takeIf { it.isNotBlank() && it != "null" }
                duration = item.optInt("duration", 0).takeIf { it > 0 }
                tags = item.optJSONArray("genres")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }
                }
                comingSoon = officialVideoEpisodes.isEmpty()
            }
        }

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
            val subbedManualEpisodes = manualEpisodes
                .filter { it.language.equals("VOSTFR", ignoreCase = true) }
                .groupBy { it.episode }
                .map { (episodeNumber, variants) ->
                    val data = mapOf(
                        "sources" to variants.map { variant ->
                            mapOf("name" to variant.source, "url" to variant.url)
                        }
                    ).toJson()
                    newEpisode(data) {
                        name = "Épisode $episodeNumber — VOSTFR"
                        episode = episodeNumber
                    }
                }
            val dubbedManualEpisodes = manualEpisodes
                .filter { it.language.equals("VF", ignoreCase = true) }
                .groupBy { it.episode }
                .map { (episodeNumber, variants) ->
                    val data = mapOf(
                        "sources" to variants.map { variant ->
                            mapOf("name" to variant.source, "url" to variant.url)
                        }
                    ).toJson()
                    newEpisode(data) {
                        name = "Épisode $episodeNumber — VF"
                        episode = episodeNumber
                    }
                }
            val officialEpisodes = officialVideoEpisodes.mapIndexed { index, (episodeTitle, episodeUrl) ->
                newEpisode(episodeUrl) {
                    name = episodeTitle
                    episode = index + 1
                }
            }
            episodes[DubStatus.Subbed] = officialEpisodes + subbedManualEpisodes
            if (dubbedManualEpisodes.isNotEmpty()) {
                episodes[DubStatus.Dubbed] = dubbedManualEpisodes
            }
            comingSoon = officialEpisodes.isEmpty() && manualEpisodes.isEmpty()
        }
    }


    private fun isOfficialStreamingHost(host: String): Boolean {
        val allowedDomains = setOf(
            "bilibili.com", "bilibili.tv",
            "crunchyroll.com", "hulu.com", "netflix.com", "primevideo.com",
            "amazon.com", "disneyplus.com", "hidive.com",
            "animationdigitalnetwork.fr", "animationdigitalnetwork.com"
        )
        return allowedDomains.any { host == it || host.endsWith(".$it") }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        // Episode data can be either one URL (AniList official links) or a bundle
        // containing multiple authorized sources for the same VF/VOSTFR episode.
        val sources: List<Pair<String, String>> = try {
            val payload = JSONObject(data)
            val array = payload.optJSONArray("sources")
            if (array != null) {
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val sourceUrl = item.optString("url").takeIf { it.isNotBlank() && it != "null" }
                        ?: return@mapNotNull null
                    val sourceName = item.optString("name").takeIf { it.isNotBlank() && it != "null" }
                        ?: "Source ${index + 1}"
                    sourceName to sourceUrl
                }
            } else listOf(name to data)
        } catch (_: Exception) {
            listOf(name to data)
        }

        var found = false
        for ((sourceName, sourceUrl) in sources) {
            val uri = try { java.net.URI(sourceUrl) } catch (_: Exception) { continue }
            if (uri.scheme != "https" || uri.host.isNullOrBlank()) continue

            val host = uri.host.lowercase()
            val path = uri.path.orEmpty().lowercase()
            val type = when {
                path.endsWith(".m3u8") -> ExtractorLinkType.M3U8
                path.endsWith(".mpd") -> ExtractorLinkType.DASH
                path.endsWith(".mp4") || path.endsWith(".m4v") ||
                    path.endsWith(".webm") || path.endsWith(".mkv") -> ExtractorLinkType.VIDEO
                else -> null
            }

            if (type != null) {
                callback(newExtractorLink(source = name, name = sourceName, url = sourceUrl, type = type))
                found = true
                continue
            }

            // Official pages only; no DRM, login, subscription, or paywall bypass.
            if (!isOfficialStreamingHost(host)) continue
            try {
                if (loadExtractor(sourceUrl, mainUrl, subtitleCallback, callback)) found = true
            } catch (_: Exception) {
                // Keep trying other sources if one extractor fails.
            }
        }
        return found
    }
}
