import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "app.paralino.core.resources.icons"
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
            api(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.foundation)
        }
    }

    jvmToolchain(21)
}

compose.resources {
    publicResClass = true
    nameOfResClass = "Res"
    packageOfResClass = "app.paralino.core.resources.icons"
}