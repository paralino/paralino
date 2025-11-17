import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.moko.resources)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
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
    namespace = "app.paralino.core"
    compileSdk = 36
}

multiplatformResources {
    resourcesPackage.set("app.paralino.core")
    resourcesClassName.set("MokoResources")
    resourcesVisibility.set(dev.icerock.gradle.MRVisibility.Public)
    iosBaseLocalizationRegion.set("en")
}