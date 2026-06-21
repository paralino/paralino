import org.gradle.kotlin.dsl.aboutLibraries
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.aboutlibraries)
}

kotlin {
    android {
        namespace = "app.paralino.core"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        compilerOptions.optIn.add("kotlin.RequiresOptIn")
        androidResources.enable = true
    }

    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "shared"
            isStatic = true

            // required so iOS app can use resource directly in Swift code
            export(project(":core-base"))
            export(project(":core-domain"))
            export(project(":core-presentation-common"))
            export(project(":core-presentation-details"))
            export(project(":core-presentation-map"))
            export(project(":core-presentation-settings"))
            export(project(":core-presentation-onboarding"))
            export(project(":core-resources-moko"))
            export(libs.moko.resources)
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(project(":core-base"))
            api(project(":core-db"))
            api(project(":core-domain"))
            api(project(":core-presentation-common"))
            api(project(":core-presentation-details"))
            api(project(":core-presentation-map"))
            api(project(":core-presentation-settings"))
            api(project(":core-presentation-onboarding"))
            api(project(":core-resources-icons"))
            api(project(":core-resources-moko"))
            api(project(":core-ui-common"))
            api(project(":core-ui-details"))
            api(project(":core-ui-map"))
            api(project(":core-ui-onboarding"))
            api(project(":core-ui-settings"))
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.android)
            api(libs.lifecycle.viewmodel.ktx)
            api(libs.activity.compose)
            api(libs.appcompat)
            api(libs.kotlinx.coroutines.android)
        }
    }

    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
        optIn.add("kotlin.time.ExperimentalTime")
    }

    jvmToolchain(21)
}

aboutLibraries {
    collect.configPath = file("../config")
    license.additionalLicenses = setOf("svg_backgrounds")
    export.prettyPrint = true
}