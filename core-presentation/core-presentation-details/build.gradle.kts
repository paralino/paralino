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
        namespace = "app.paralino.core.presentation.details"
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
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            // koin only used here because of BaseRepository, remove at some point to slim down dependencies
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(project(":core-base"))
            implementation(project(":core-db"))
            implementation(project(":core-domain"))
            implementation(project(":core-presentation-common"))
            implementation(project(":core-resources-icons"))
            implementation(project(":core-resources-moko"))
        }

        androidMain.dependencies {
            implementation(libs.appcompat)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.lifecycle.viewmodel.ktx)
            implementation(libs.activity.compose)
        }
    }

    jvmToolchain(21)
}