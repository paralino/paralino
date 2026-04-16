import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "app.paralino.core.domain"
        compileSdk = 36
        minSdk = 29
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        compilerOptions.optIn.add("kotlin.RequiresOptIn")
        androidResources.enable = true
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings)
            implementation(libs.network.ktor.client.core)
            implementation(libs.network.ktor.client.logging)
            implementation(libs.network.ktor.content.negotiation)
            implementation(libs.network.ktor.serialization)
            // koin only used in BaseRepository, remove at some point to slim down dependencies
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(project(":core-resources-moko"))
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

compose.resources {
    publicResClass = true
    nameOfResClass = "Res"
    packageOfResClass = "app.paralino.core.domain.resources"
}