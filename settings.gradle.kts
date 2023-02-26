dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")
    }
    versionCatalogs {
        create("libs") {
            plugin("lsplugin-apktransform", "org.lsposed.lsplugin.apktransform").version("1.2")
        }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.android") version "1.8.10"
    }
}

rootProject.name = "TwiFucker"

include(":app")
