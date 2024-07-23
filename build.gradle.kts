
plugins {
    // Use the plugin versions from the version catalog
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.moko.resources) apply false
    alias(libs.plugins.buildkonfig) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx/")
    }
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}