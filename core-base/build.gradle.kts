import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.paralino.core.base"
        compileSdk = 36
        minSdk = 29
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        compilerOptions.optIn.add("kotlin.RequiresOptIn")
    }

    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.moko.resources)
            implementation(libs.moko.resources.compose)
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(libs.appcompat)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}