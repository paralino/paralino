plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.moko.resources)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "moko"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.moko.resources)
            api(libs.moko.resources.compose)
        }
    }
}

android {
    namespace = "app.paralino"
    compileSdk = 34
    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

multiplatformResources {
    resourcesPackage.set("app.paralino.core")
    resourcesClassName.set("MokoResources")
    resourcesVisibility.set(dev.icerock.gradle.MRVisibility.Public)
    iosBaseLocalizationRegion.set("en")
}