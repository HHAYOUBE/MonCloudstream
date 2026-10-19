package com.hhayoube.senpaistream

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SenpaiStreamPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(SenpaiStreamProvider())
        registerMainAPI(GeneralCatalogProvider())
        registerMainAPI(PublicDomainFilmsProvider())
    }
}
