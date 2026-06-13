plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "com.google.protobuf" -> if (requested.name.startsWith("protobuf-")) {
                    useVersion("4.28.3")
                    because("Avoid vulnerable protobuf versions while matching Android test platform tooling")
                }
                "io.netty" -> if (requested.name.startsWith("netty-")) {
                    useVersion("4.1.135.Final")
                    because("Avoid vulnerable Netty versions pulled by Android test platform tooling")
                }
                "org.apache.commons" -> when (requested.name) {
                    "commons-lang3" -> {
                        useVersion("3.18.0")
                        because("Avoid vulnerable Commons Lang versions pulled by Android lint tooling")
                    }
                }
                "org.apache.httpcomponents" -> when (requested.name) {
                    "httpclient" -> {
                        useVersion("4.5.14")
                        because("Avoid vulnerable HttpClient versions pulled by Android lint tooling")
                    }
                }
                "org.bouncycastle" -> when (requested.name) {
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
