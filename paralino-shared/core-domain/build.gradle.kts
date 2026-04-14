import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.paralino.core.domain"
        compileSdk = 36
        minSdk = 29
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        compilerOptions.optIn.add("kotlin.RequiresOptIn")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings)
            implementation(libs.network.ktor.client.core)
            implementation(libs.network.ktor.client.logging)
            implementation(libs.network.ktor.content.negotiation)
            implementation(libs.network.ktor.serialization)
            implementation(project(":moko"))
            implementation(project(":core-base"))
            implementation(project(":core-db"))
        }

        androidMain.dependencies {
            implementation(libs.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.network.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.network.ktor.client.darwin)
        }
    }

    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
        optIn.add("kotlin.time.ExperimentalTime")
    }

    jvmToolchain(21)
}