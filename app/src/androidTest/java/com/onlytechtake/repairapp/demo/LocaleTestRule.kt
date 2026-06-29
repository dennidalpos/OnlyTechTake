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
                val resources = context.resources
                val configuration = resources.configuration

                deviceLocale = configuration.locales[0]

                System.setProperty("forceEnglish", "true")
                Locale.setDefault(locale)
                configuration.setLocale(locale)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)

                try {
                    base.evaluate()
                } finally {
                    System.clearProperty("forceEnglish")
                    deviceLocale?.let {
                        Locale.setDefault(it)
                        configuration.setLocale(it)
                        @Suppress("DEPRECATION")
                        resources.updateConfiguration(configuration, resources.displayMetrics)
                    }
                }
            }
        }
}
