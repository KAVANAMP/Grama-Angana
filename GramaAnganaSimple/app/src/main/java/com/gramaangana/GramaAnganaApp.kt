package com.gramaangana

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class GramaAnganaApp : Application() {

    companion object {
        lateinit var instance: GramaAnganaApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AndroidThreeTen.init(this)
    }
}
