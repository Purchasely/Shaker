pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.purchasely.io") }
    }
    versionCatalogs {
        create("libs") {
            from(files("android/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Shaker"

include(":app")
project(":app").projectDir = file("android/app")
