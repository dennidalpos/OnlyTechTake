plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

spotless {
    val generatedTargets =
        arrayOf(
            "**/build/**",
            ".gradle/**",
            ".idea/**",
            ".kotlin/**",
            "app/schemas/**",
            "asset/**",
            "demo/**",
        )

    kotlin {
        target("**/*.kt")
        targetExclude(*generatedTargets)
        ktlint("1.8.0")
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude(*generatedTargets)
        ktlint("1.8.0")
    }

    format("misc") {
        target(
            "**/*.md",
            "**/*.json",
            "**/*.xml",
            "**/*.yml",
            "**/*.yaml",
            "**/*.properties",
        )
        targetExclude(*generatedTargets)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

subprojects {
    dependencyLocking {
        lockAllConfigurations()
    }

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "androidx.appcompat" -> {
                    if (requested.name.startsWith("appcompat")) {
                        useVersion("1.7.1")
                        because("Consistent appcompat version across all configurations")
                    }
                }

                "com.google.android.material" -> {
                    if (requested.name == "material") {
                        useVersion("1.14.0")
                        because("Consistent material version across all configurations")
                    }
                }

                "com.google.protobuf" -> {
                    if (requested.name.startsWith("protobuf-")) {
                        useVersion("4.35.1")
                        because("Avoid vulnerable protobuf versions while matching Android test platform tooling")
                    }
                }

                "io.netty" -> {
                    if (requested.name.startsWith("netty-")) {
                        useVersion("4.1.135.Final")
                        because("Avoid vulnerable Netty versions pulled by Android test platform tooling")
                    }
                }

                "org.apache.commons" -> {
                    when (requested.name) {
                        "commons-lang3" -> {
                            useVersion("3.20.0")
                            because("Avoid vulnerable Commons Lang versions pulled by Android lint tooling")
                        }
                    }
                }

                "org.apache.httpcomponents" -> {
                    when (requested.name) {
                        "httpclient" -> {
                            useVersion("4.5.14")
                            because("Avoid vulnerable HttpClient versions pulled by Android lint tooling")
                        }
                    }
                }

                "org.bouncycastle" -> {
                    when (requested.name) {
                        "bcpkix-jdk18on",
                        "bcprov-jdk18on",
                        -> {
                            useVersion("1.84")
                            because("Avoid vulnerable Bouncy Castle versions pulled by Android lint tooling")
                        }
                    }
                }
            }
        }
    }
}
