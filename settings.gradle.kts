rootProject.name = "Paralino"

include(":androidApp")
include(":shared")
include(":moko")
include(":appwrite-android")
include(":lazysodium-android")
project(":appwrite-android").projectDir = File("appwrite-android/library")
project(":lazysodium-android").projectDir = File("lazysodium-android/app")

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