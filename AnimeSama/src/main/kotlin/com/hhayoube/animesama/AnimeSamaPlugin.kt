package com.hhayoube.animesama

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AnimeSamaPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(AnimeSamaProvider())
    }
}
