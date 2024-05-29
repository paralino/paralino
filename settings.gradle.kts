rootProject.name = "Paralino"

include(":androidApp")
include(":shared")
include(":moko")
include(":appwrite-android")
project(":appwrite-android").projectDir = File("appwrite-android/library")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}