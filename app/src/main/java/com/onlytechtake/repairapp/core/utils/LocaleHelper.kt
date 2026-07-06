package com.onlytechtake.repairapp.core.utils

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleForcer {
    var forceEnglish: Boolean = false
}

object LocaleHelper {
    fun isEnglishForced(): Boolean =
        LocaleForcer.forceEnglish || System.getProperty("forceEnglish") == "true"

    fun applyLocale(context: Context): Context {
        if (!isEnglishForced()) return context

        val locale = Locale.ENGLISH
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Legacy method for updating configuration in place.
     * Still needed for some system-level resources that don't respect the context wrapper.
     */
    @Suppress("DEPRECATION")
    fun applyLocaleLegacy(context: Context) {
        if (!isEnglishForced()) return

        val locale = Locale.ENGLISH
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration
        val localeList = LocaleList(locale)

        config.setLocales(localeList)
        config.setLayoutDirection(locale)

        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
