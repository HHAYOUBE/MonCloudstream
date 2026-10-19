package com.hhayoube.senpaistream

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * One CloudStream provider that combines the existing metadata/catalogue providers.
 * Playback is delegated only to the existing official-source resolver and the
 * dedicated public-domain film provider.
 */
class UnifiedCatalogueProvider : MainAPI() {
    private val animeProvider = SenpaiStreamProvider()
    private val metadataProvider = GeneralCatalogProvider()
    private val publicDomainProvider = PublicDomainFilmsProvider()

    override var mainUrl = SenpaiStreamConfig.baseUrl
    override var name = "SenpaiStream"
    override var lang = "fr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime, TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "anime:TRENDING_DESC" to "Anime · Tendances",
        "anime:POPULARITY_DESC" to "Anime · Les plus populaires",
        "anime:SCORE_DESC" to "Anime · Les mieux notés",
        "anime:START_DATE_DESC" to "Anime · Nouveautés",
        "film:top" to "Films · Populaires (métadonnées)",
        "series:top" to "Séries · Populaires (métadonnées)",
        "public:popular" to "Films · Domaine public",
        "public:recent" to "Films · Domaine public récemment ajoutés"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val (provider, childData) = when {
            request.data.startsWith("anime:") ->
                animeProvider to request.data.substringAfter("anime:")
            request.data.startsWith("film:") ->
                metadataProvider to "movie:top"
            request.data.startsWith("series:") ->
                metadataProvider to "series:top"
            request.data.startsWith("public:") ->
                publicDomainProvider to request.data.substringAfter("public:")
            else -> return newHomePageResponse(request.name, emptyList())
        }

        return provider.getMainPage(
            page,
            MainPageRequest(
                name = request.name,
                data = childData,
                horizontalImages = request.horizontalImages
            )
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()

        val results = mutableListOf<SearchResponse>()
        val errors = mutableListOf<String>()

        try {
            results += animeProvider.search(term)
        } catch (e: Exception) {
            errors += "anime: " + (e.message ?: "source indisponible")
        }

        try {
            results += metadataProvider.search(term)
        } catch (e: Exception) {
            errors += "films/séries: " + (e.message ?: "source indisponible")
        }

        try {
            results += publicDomainProvider.search(term)
        } catch (e: Exception) {
            errors += "domaine public: " + (e.message ?: "source indisponible")
        }

        if (results.isEmpty() && errors.size == 3) {
            throw ErrorLoadingException("Recherche globale impossible : " + errors.joinToString(" ; "))
        }

        return results.distinctBy { it.url }.take(MAX_SEARCH_RESULTS)
    }

    override suspend fun load(url: String): LoadResponse? = when {
        Regex("/anilist/\\d+").containsMatchIn(url) ->
            animeProvider.load(url)

        Regex("/meta/(movie|series)/").containsMatchIn(url) ->
            metadataProvider.load(url)

        url.startsWith("https://archive.org/details/") ||
            url.startsWith("http://archive.org/details/") ->
            publicDomainProvider.load(url)

        else -> null
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val value = data.trim()

        // AniList episode pages and anime-movie source bundles are handled by the
        // existing resolver, which restricts page links to known official domains.
        if (value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("{")) {
            return try {
                animeProvider.loadLinks(value, isCasting, subtitleCallback, callback)
            } catch (_: Exception) {
                false
            }
        }

        // Public-domain item IDs are the playback data for Internet Archive films.
        if (value.matches(Regex("[A-Za-z0-9._-]+"))) {
            return try {
                publicDomainProvider.loadLinks(value, isCasting, subtitleCallback, callback)
            } catch (_: Exception) {
                false
            }
        }

        // Cinemeta has metadata only; it does not supply playable media.
        return false
    }

    private companion object {
        const val MAX_SEARCH_RESULTS = 100
    }
}
