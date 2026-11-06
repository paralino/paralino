import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "app.paralino.core.db"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        compilerOptions.optIn.add("kotlin.RequiresOptIn")
    }

    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.moko.resources)
            implementation(libs.moko.resources.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.sqlite.bundled)
            implementation(project(":core-base"))
        }

        androidMain.dependencies {
            implementation(libs.appcompat)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.preference.ktx)
            implementation(libs.androidx.security.crypto.ktx)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    // Fix for JVM 64KB method limit since room3 uses suspend functions everywhere,
    // and it can throw Method to Large errors
    // Issue: https://issuetracker.google.com/issues/493708172
    arg("room.validationSplitSize", "100") // default is 300
}
