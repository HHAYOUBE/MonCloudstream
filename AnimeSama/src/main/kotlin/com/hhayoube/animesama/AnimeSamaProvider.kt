package com.hhayoube.animesama

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.nicehttp.RequestBodyTypes
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Recherche et fiches d'anime via l'API publique AniList.
 * Cette intégration fournit des métadonnées, pas de flux vidéo.
 */
class AnimeSamaProvider : MainAPI() {
    override var mainUrl = AnimeSamaConfig.baseUrl
    override var name = "AnimeSama"
    override var lang = "fr"
    override val hasMainPage = false
    override val supportedTypes = setOf(TvType.Anime)

    private val endpoint = "https://graphql.anilist.co"

    private suspend fun request(query: String, variables: Map<String, Any?>): JSONObject {
        val body = mapOf("query" to query, "variables" to variables)
            .toJson()
            .toRequestBody(RequestBodyTypes.JSON.toMediaTypeOrNull())
        return JSONObject(
            app.post(
                endpoint,
                requestBody = body,
                headers = mapOf("Accept" to "application/json")
            ).text
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()

        val gql = """
            query ($search: String) {
              Page(page: 1, perPage: 20) {
                media(search: $search, type: ANIME, isAdult: false, sort: SEARCH_MATCH) {
                  id
                  title { romaji english native }
                  coverImage { large extraLarge }
                  startDate { year }
                  episodes
                  format
                }
              }
            }
        """.trimIndent()

        val root = try {
            request(gql, mapOf("search" to term))
        } catch (_: Exception) {
            return emptyList()
        }

        val media = root.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media")
            ?: return emptyList()

        return (0 until media.length()).mapNotNull { index ->
            val item = media.optJSONObject(index) ?: return@mapNotNull null
            val titles = item.optJSONObject("title") ?: JSONObject()
            val title = titles.optString("english").takeIf { it.isNotBlank() && it != "null" }
                ?: titles.optString("romaji").takeIf { it.isNotBlank() && it != "null" }
                ?: titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
                ?: return@mapNotNull null
            val id = item.optInt("id", 0)
            if (id <= 0) return@mapNotNull null
            val poster = item.optJSONObject("coverImage")?.optString("extraLarge")
                ?.takeIf { it.isNotBlank() && it != "null" }
                ?: item.optJSONObject("coverImage")?.optString("large")
            newAnimeSearchResponse(title, "$mainUrl/anilist/$id", TvType.Anime) {
                posterUrl = poster?.takeIf { it.isNotBlank() && it != "null" }
                year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
                item.optInt("episodes", 0).takeIf { it > 0 }?.let { episodes[com.lagradost.cloudstream3.DubStatus.Subbed] = it }
                otherName = titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = Regex("/anilist/(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Regex("(?:anime/|id=)(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return null

        val gql = """
            query ($id: Int) {
              Media(id: $id, type: ANIME) {
                id
                title { romaji english native }
                description(asHtml: false)
                coverImage { large extraLarge }
                bannerImage
                startDate { year }
                episodes
                duration
                genres
                synonyms
                siteUrl
              }
            }
        """.trimIndent()

        val root = try {
            request(gql, mapOf("id" to id))
        } catch (_: Exception) {
            return null
        }
        val item = root.optJSONObject("data")?.optJSONObject("Media") ?: return null
        val titles = item.optJSONObject("title") ?: JSONObject()
        val title = titles.optString("english").takeIf { it.isNotBlank() && it != "null" }
            ?: titles.optString("romaji").takeIf { it.isNotBlank() && it != "null" }
            ?: return null
        val poster = item.optJSONObject("coverImage")?.optString("extraLarge")
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?: item.optJSONObject("coverImage")?.optString("large")
        val description = item.optString("description").takeIf { it.isNotBlank() && it != "null" }

        return newAnimeLoadResponse(title, "$mainUrl/anilist/$id", TvType.Anime) {
            engName = titles.optString("english").takeIf { it.isNotBlank() && it != "null" }
            japName = titles.optString("native").takeIf { it.isNotBlank() && it != "null" }
            posterUrl = poster
            backgroundPosterUrl = item.optString("bannerImage").takeIf { it.isNotBlank() && it != "null" }
            year = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }
            plot = description
            duration = item.optInt("duration", 0).takeIf { it > 0 }
            tags = item.optJSONArray("genres")?.let { array ->
                (0 until array.length()).mapNotNull { i -> array.optString(i).takeIf { it.isNotBlank() } }
            }
            synonyms = item.optJSONArray("synonyms")?.let { array ->
                (0 until array.length()).mapNotNull { i -> array.optString(i).takeIf { it.isNotBlank() } }
            }
            comingSoon = false
        }
    }
}
