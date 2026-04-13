
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.paralino.core.base"
        compileSdk = 36
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.moko.resources)
            api(libs.moko.resources.compose)
        }

        androidMain.dependencies {
            api(libs.appcompat)
            api(libs.androidx.core.ktx)
            api(libs.kotlinx.coroutines.android)
        }

        iosMain.get().dependsOn(commonMain.get())
    }
}