package com.hhayoube.senpaistream

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SenpaiStreamPlugin : Plugin() {
    override fun load(context: Context) {
        // A single visible provider; the other classes are internal data/playback adapters.
        registerMainAPI(UnifiedCatalogueProvider())
    }
}
