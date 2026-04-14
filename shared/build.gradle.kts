import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.gradle.kotlin.dsl.aboutLibraries
import org.jetbrains.kotlin.daemon.common.trimQuotes
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.multiplatformLibrary)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.aboutlibraries)
}

kotlin {
    android {
        namespace = "app.paralino.core"
        compileSdk = 36
        minSdk = 29
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        compilerOptions.optIn.add("kotlin.RequiresOptIn")
        androidResources.enable = true
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "shared"
            isStatic = true

            // required so iOS app can use resource directly in Swift code
            export(project(":moko"))
            export(libs.moko.resources)
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material)
            api(libs.jetbrains.compose.components.resources)
            api(project(":moko"))
            api(project(":core-base"))
            api(project(":core-db"))
            api(project(":core-domain"))
            api(project(":core-presentation"))
            api(project(":core-ui-common"))
            api(project(":core-ui-map"))
            api(project(":core-ui-settings"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings)
            api(libs.network.ktor.client.core)
            api(libs.network.ktor.client.logging)
            api(libs.network.ktor.content.negotiation)
            implementation(libs.network.ktor.serialization)
            implementation(libs.aboutLibraries.core)
            implementation(libs.aboutLibraries.compose)
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.android)
            api(libs.lifecycle.viewmodel.ktx)
            api(libs.activity.compose)
            api(libs.appcompat)
            api(libs.androidx.core.ktx)
            api(libs.kotlinx.coroutines.android)
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

buildkonfig {
    packageName = "app.paralino.core"
    exposeObjectWithName = "BuildKonfig"

    val propsFile = rootProject.file("user.properties")
    if (!propsFile.exists()) {
        throw FileNotFoundException("user.properties file not found")
    }

    val props = Properties()
    props.load(FileInputStream(propsFile))

    defaultConfigs {}

    targetConfigs {
        create("ios") {
            buildConfigField(FieldSpec.Type.STRING, "glitchTipDSN", props.getProperty("glitchTipDSNiOS").trimQuotes())
        }
    }
}

aboutLibraries {
    collect.configPath = file("../config")
    license.additionalLicenses = setOf("svg_backgrounds")
    export.prettyPrint = true
}