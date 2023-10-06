plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("8.1.2").apply(false)
    id("com.android.library").version("8.1.2").apply(false)
    id("org.jetbrains.compose").version("1.5.2").apply(false)
    id("com.mikepenz.aboutlibraries.plugin").version("10.5.1").apply(false)
    id("app.cash.sqldelight").version("2.0.0").apply(false)
    id("dev.icerock.mobile.multiplatform-resources").version("0.23.0").apply(false)
    kotlin("android").version("1.9.10").apply(false)
    kotlin("multiplatform").version("1.9.10").apply(false)
    kotlin("plugin.serialization").version("1.9.10").apply(false)
}

// region kotlinx atomicfu error

// at some point build failed with new version
// remove this after fix and if build works without workaround
// https://slack-chats.kotlinlang.org/t/12093271/hi-folks-i-am-trying-to-run-the-kmm-compose-multiplatform-pr#1aebb3a5-2e7e-4dd6-973c-f5763e1559a3
//buildscript {
//    dependencies {
//        // Use the same version in the error
//        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.17.3")
//    }
//}
//
//allprojects {
//    apply(plugin = "kotlinx-atomicfu")
//}

// endregion

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx/")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}