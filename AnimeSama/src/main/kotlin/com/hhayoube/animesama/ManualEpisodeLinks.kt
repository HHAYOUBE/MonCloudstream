package com.hhayoube.animesama

/**
 * Variantes d'épisodes provenant de sources autorisées.
 *
 * Une entrée représente une source pour un épisode et une langue.
 * Plusieurs entrées partageant le même numéro/langue deviennent des choix de source
 * dans CloudStream. Ne renseigner que des liens directs autorisés ou des pages officielles.
 */
data class ManualEpisodeVariant(
    val episode: Int,
    val language: String, // "VF" ou "VOSTFR"
    val source: String,
    val url: String,
)

object ManualEpisodeLinks {
    // Clé = identifiant AniList affiché dans la fiche (/anilist/ID).
    // Les listes restent vides tant qu'aucune source autorisée vérifiée n'est configurée.
    val links: Map<Int, List<ManualEpisodeVariant>> = mapOf(
        // Exemple :
        // 21 to listOf(
        //     ManualEpisodeVariant(1, "VF", "Lecteur officiel", "https://exemple.org/episode-1.mp4"),
        //     ManualEpisodeVariant(1, "VF", "Lecteur alternatif", "https://cdn.exemple.org/episode-1.m3u8"),
        //     ManualEpisodeVariant(1, "VOSTFR", "Lecteur officiel", "https://exemple.org/episode-1-sub.mp4"),
        // ),
    )
}
