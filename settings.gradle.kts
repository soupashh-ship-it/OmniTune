@file:Suppress("UnstableApiUsage")

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
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "OmniTune"
include(":app")
include(":innertube")
include(":kugou")
include(":lrclib")
include(":lastfm")
include(":simpmusic")
include(":betterlyrics")
include(":kizzy")
include(":canvas")
