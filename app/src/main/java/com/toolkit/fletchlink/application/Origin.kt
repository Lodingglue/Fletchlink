package com.toolkit.fletchlink.application

import android.app.Application

class Origin : Application() {

    companion object {
        lateinit var instance: Origin
            private set
    }

    override fun onCreate() {
        super.onCreate()

            instance = this
    }
}
