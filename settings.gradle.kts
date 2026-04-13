rootProject.name = "Paralino"

include(":androidApp")
include(":shared")
include(":moko")
include(":appwrite-android")
include(":lazysodium-android")
include(":core-base")
include(":core-db")
include(":core-domain")
include(":core-presentation")
include(":core-ui-common")
include(":core-ui-map")
include(":core-ui-settings")

project(":appwrite-android").projectDir = File("appwrite-android/library")
project(":lazysodium-android").projectDir = File("lazysodium-android/app")
project(":core-base").projectDir = File("paralino-shared/core-base")
project(":core-db").projectDir = File("paralino-shared/core-db")
project(":core-domain").projectDir = File("paralino-shared/core-domain")
project(":core-presentation").projectDir = File("paralino-shared/core-presentation")
project(":core-ui-common").projectDir = File("paralino-shared/core-ui/core-ui-common")
project(":core-ui-map").projectDir = File("paralino-shared/core-ui/core-ui-map")
project(":core-ui-settings").projectDir = File("paralino-shared/core-ui/core-ui-settings")

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