
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.moko.resources)
    // Needed for composable extension
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "app.paralino.core.resources.moko"
        compileSdk = libs.versions.compileSdk.get().toInt()
    }

    listOf(
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

multiplatformResources {
    resourcesPackage.set("app.paralino.core")
    resourcesClassName.set("MokoResources")
    resourcesVisibility.set(dev.icerock.gradle.MRVisibility.Public)
    iosBaseLocalizationRegion.set("en")
}