package com.hhayoube.senpaistream

/**
 * Liens vidéo autorisés ajoutés manuellement.
 * Remplace les listes vides par les URL directes de tes propres vidéos ou d'une source autorisée.
 * Clé = identifiant AniList affiché dans la fiche (/anilist/ID).
 */
object ManualEpisodeLinks {
    val links: Map<Int, List<String>> = mapOf(
        // Exemple à compléter :
        // 21 to listOf("https://exemple.org/video/episode-1.mp4", "https://exemple.org/video/episode-2.m3u8"),
    )
}
