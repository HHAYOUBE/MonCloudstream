package com.hhayoube.monextension

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType

/**
 * Point de départ pour une source que tu es autorisé à utiliser.
 *
 * Pour rendre la recherche et les fiches fonctionnelles, il faudra adapter
 * search() et load() à la structure publique du site choisi.
 */
class MonExtensionProvider : MainAPI() {
    override var mainUrl = MonExtensionConfig.baseUrl
    override var name = "MonExtension"
    override var lang = "fr"
    override val hasMainPage = false
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun search(query: String): List<SearchResponse> {
        // TODO: analyser la page de recherche de la source autorisée.
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        // TODO: analyser la fiche du contenu et ses métadonnées.
        return null
    }
}
