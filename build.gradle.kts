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
                    useVersion("3.25.9")
                    because("Avoid vulnerable protobuf versions pulled by Android test platform tooling")
                }
                "io.netty" -> if (requested.name.startsWith("netty-")) {
                    useVersion("4.1.135.Final")
                    because("Avoid vulnerable Netty versions pulled by Android test platform tooling")
                }
            }
        }
    }
}
