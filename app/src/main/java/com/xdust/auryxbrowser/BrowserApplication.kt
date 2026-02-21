package com.xdust.auryxbrowser

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * Custom [Application] implementation used to configure global application
 * behaviour such as theme settings.
 */
class BrowserApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Follow the system day/night setting automatically.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}