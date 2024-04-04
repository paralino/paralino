import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.daemon.common.trimQuotes
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight")
    id("org.jetbrains.compose")
    id("com.codingfeline.buildkonfig")
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries {
            framework {
                baseName = "shared"
                isStatic = true
                linkerOpts.add("-lsqlite3")

                // required so iOS app can use resource directly in Swift code
                export(project(":moko"))
                export("dev.icerock.moko:resources:0.23.0")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                api(project(":moko"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0-RC.2")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                api("app.cash.sqldelight:coroutines-extensions:2.0.1")
                api("app.cash.sqldelight:primitive-adapters:2.0.1")
                implementation("com.russhwolf:multiplatform-settings-coroutines:1.1.1")
                implementation("com.russhwolf:multiplatform-settings:1.1.1")
                api("io.insert-koin:koin-core:3.5.3")
            }
        }

        val androidMain by getting {
            dependencies {
                dependsOn(commonMain)
                api("io.insert-koin:koin-android:3.5.3")
                api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
                implementation("androidx.preference:preference-ktx:1.2.1")
                implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
                implementation("app.cash.sqldelight:android-driver:2.0.1")
                api("androidx.activity:activity-compose:1.8.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

// Workaround for compose multiplatform not using latest kotlin version
// this silences the error and it should be fine for minor versions
// sqldelight uses 1.8.22 which causes build problems
// remove after compose catches up
//compose {
//    kotlinCompilerPlugin.set(dependencies.compiler.forKotlin("1.8.20"))
//    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.8.22")
//}

android {
    namespace = "app.paralino"
    compileSdk = 34

    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

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

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("app.paralino.core.data.db")
        }
    }
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

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "serverUrl", props.getProperty("serverUrl").trimQuotes())
        buildConfigField(FieldSpec.Type.STRING, "serverKey", props.getProperty("serverKey").trimQuotes())
    }

    targetConfigs {
        create("android") {
            buildConfigField(FieldSpec.Type.STRING, "glitchTipDSN", props.getProperty("glitchTipDSNAndroid").trimQuotes())
            buildConfigField(FieldSpec.Type.STRING, "mapboxAccessToken", props.getProperty("MAPBOX_ACCESS_TOKEN").trimQuotes())
        }
        create("ios") {
            buildConfigField(FieldSpec.Type.STRING, "glitchTipDSN", props.getProperty("glitchTipDSNiOS").trimQuotes())
        }
    }
}