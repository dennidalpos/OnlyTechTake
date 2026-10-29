package com.onlytechtake.repairapp.core.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLocaleManager {
    private var forceEnglish: Boolean = false

    fun isForceEnglish(): Boolean = forceEnglish

    fun initialize(
        context: Context,
        forceEnglishFromIntent: Boolean,
    ) {
        if (forceEnglishFromIntent) {
            forceEnglish = true
        }

        if (forceEnglish) {
            setLocale(context, "en")
        }
    }

    fun getLocalizedContext(context: Context): Context {
        if (!forceEnglish) return context

        val locale = Locale.ENGLISH
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    private fun setLocale(
        context: Context,
        languageTag: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
            localeManager.applicationLocales = LocaleList.forLanguageTags(languageTag)
        } else {
            // For older versions without AppCompat, we rely on getLocalizedContext
            // applied in the Activity or via custom wrapping if needed.
            // In this app, we primarily use getLocalizedContext in MainActivity.
        }
    }
}
