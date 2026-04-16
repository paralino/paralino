import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.aboutlibraries)
}

kotlin {
    android {
        namespace = "app.paralino.core.ui.settings"
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
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.kotlinx.datetime)
            implementation(libs.aboutLibraries.core)
            implementation(libs.aboutLibraries.compose)
            implementation(project(":moko"))
            implementation(project(":core-base"))
            implementation(project(":core-db"))
            implementation(project(":core-domain"))
            implementation(project(":core-presentation-common"))
            implementation(project(":core-presentation-details"))
            implementation(project(":core-presentation-settings"))
            implementation(project(":core-resources-icons"))
            implementation(project(":core-ui-common"))
            implementation(project(":core-ui-details"))
        }

        androidMain.dependencies {
            implementation(libs.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.lifecycle.viewmodel.ktx)
            implementation(libs.activity.compose)
        }
    }

    jvmToolchain(21)
}

compose.resources {
    publicResClass = true
    nameOfResClass = "Res"
    packageOfResClass = "app.paralino.core.ui.settings.resources"
}