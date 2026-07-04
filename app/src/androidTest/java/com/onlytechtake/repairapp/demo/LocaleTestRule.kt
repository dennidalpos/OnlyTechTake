package com.onlytechtake.repairapp.demo

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.Locale

class LocaleTestRule(
    defaultLanguage: String = "en",
    defaultCountry: String = "US",
) : TestRule {
    val locale: Locale by lazy {
        val args = InstrumentationRegistry.getArguments()
        val lang = args.getString("locale") ?: defaultLanguage
        val country = args.getString("country") ?: defaultCountry

        Locale
            .Builder()
            .setLanguage(lang)
            .apply { if (country.isNotEmpty()) setRegion(country) }
            .build()
    }
    private var deviceLocale: Locale? = null

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val appCtx = context.applicationContext
                val testCtx = InstrumentationRegistry.getInstrumentation().context

                deviceLocale = context.resources.configuration.locales[0]

                val isForceEnglish = locale.language == "en"
                System.setProperty("forceEnglish", isForceEnglish.toString())
                com.onlytechtake.repairapp.core.design.LocaleForcer.forceEnglish = isForceEnglish
                Locale.setDefault(locale)

                updateContextLocale(context, locale)
                updateContextLocale(appCtx, locale)
                updateContextLocale(testCtx, locale)

                try {
                    base.evaluate()
                } finally {
                    System.clearProperty("forceEnglish")
                    com.onlytechtake.repairapp.core.design.LocaleForcer.forceEnglish = false
                    deviceLocale?.let {
                        Locale.setDefault(it)
                        updateContextLocale(context, it)
                        updateContextLocale(appCtx, it)
                    }
                }
            }

            private fun updateContextLocale(
                context: android.content.Context,
                locale: Locale,
            ) {
                val resources = context.resources
                val configuration = resources.configuration
                configuration.setLocales(android.os.LocaleList(locale))
                configuration.setLayoutDirection(locale)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)
            }
        }
}
