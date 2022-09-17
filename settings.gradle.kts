pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Paralino"
include(":androidApp")
include(":shared")
include(":appwrite")
project(":appwrite").projectDir = File("appwrite-android/library")