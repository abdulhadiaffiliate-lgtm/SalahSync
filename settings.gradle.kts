pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SalahSync"
include(":core")
// The Android app needs the Android SDK. Pass -PcoreOnly to build just the
// pure-Kotlin prayer engine on a machine without it.
if (!providers.gradleProperty("coreOnly").isPresent) {
    include(":app")
}
