package com.hhayoube.senpaistream

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType

/**
 * Squelette CloudStream. La recherche et le chargement restent volontairement
 * non implémentés : n'ajouter que des intégrations autorisées.
 */
class SenpaiStreamProvider : MainAPI() {
    override var mainUrl = SenpaiStreamConfig.baseUrl
    override var name = "SenpaiStream"
    override var lang = "fr"
    override val hasMainPage = false
    override val supportedTypes = setOf(TvType.Anime, TvType.TvSeries, TvType.Movie)

    override suspend fun search(query: String): List<SearchResponse> = emptyList()

    override suspend fun load(url: String): LoadResponse? = null
}
