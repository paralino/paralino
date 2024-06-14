plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("8.3.2").apply(false)
    id("com.android.library").version("8.3.2").apply(false)
    id("org.jetbrains.compose").version("1.6.11").apply(false)
    id("com.mikepenz.aboutlibraries.plugin").version("10.5.1").apply(false)
    id("app.cash.sqldelight").version("2.0.1").apply(false)
    id("dev.icerock.mobile.multiplatform-resources").version("0.23.0").apply(false)
    id("com.codingfeline.buildkonfig").version("0.15.1").apply(false)
    kotlin("android").version("1.9.24").apply(false)
    kotlin("multiplatform").version("1.9.24").apply(false)
    kotlin("plugin.serialization").version("1.9.24").apply(false)
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