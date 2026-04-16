rootProject.name = "Paralino"

include(":androidApp")
include(":shared")
include(":appwrite-android")
include(":lazysodium-android")
include(":core-base")
include(":core-db")
include(":core-domain")
include(":core-presentation-common")
include(":core-presentation-details")
include(":core-presentation-map")
include(":core-presentation-settings")
include(":core-presentation-onboarding")
include(":core-resources-icons")
include(":core-resources-moko")
include(":core-ui-common")
include(":core-ui-details")
include(":core-ui-map")
include(":core-ui-onboarding")
include(":core-ui-settings")

project(":appwrite-android").projectDir = File("appwrite-android/library")
project(":lazysodium-android").projectDir = File("lazysodium-android/app")
project(":core-base").projectDir = File("paralino-shared/core-base")
project(":core-db").projectDir = File("paralino-shared/core-db")
project(":core-domain").projectDir = File("paralino-shared/core-domain")
project(":core-presentation-common").projectDir = File("paralino-shared/core-presentation/core-presentation-common")
project(":core-presentation-details").projectDir = File("paralino-shared/core-presentation/core-presentation-details")
project(":core-presentation-map").projectDir = File("paralino-shared/core-presentation/core-presentation-map")
project(":core-presentation-settings").projectDir = File("paralino-shared/core-presentation/core-presentation-settings")
project(":core-presentation-onboarding").projectDir = File("paralino-shared/core-presentation/core-presentation-onboarding")
project(":core-resources-icons").projectDir = File("paralino-shared/core-resources/core-resources-icons")
project(":core-resources-moko").projectDir = File("paralino-shared/core-resources/core-resources-moko")
project(":core-ui-common").projectDir = File("paralino-shared/core-ui/core-ui-common")
project(":core-ui-details").projectDir = File("paralino-shared/core-ui/core-ui-details")
project(":core-ui-map").projectDir = File("paralino-shared/core-ui/core-ui-map")
project(":core-ui-onboarding").projectDir = File("paralino-shared/core-ui/core-ui-onboarding")
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