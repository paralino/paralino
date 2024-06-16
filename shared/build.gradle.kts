import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.daemon.common.trimQuotes
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.moko.resources)
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

                // required so iOS app can use resource directly in Swift code
                export(project(":moko"))
                export(libs.moko.resources)
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            // using api here so we can access resources in android as well
            api(compose.components.resources)
            api(project(":moko"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
            api(libs.sqldelight.coroutines)
            api(libs.sqldelight.adapters)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings)
            api(libs.koin.core)
        }

        androidMain.dependencies {
            api(libs.koin.android)
            api(libs.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.preference.ktx)
            implementation(libs.androidx.security.crypto.ktx)
            implementation(libs.sqldelight.driver.android)
            api(libs.activity.compose)
            api(libs.appcompat)
            api(libs.androidx.core.ktx)
        }

        iosMain.get().dependsOn(commonMain.get())
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
        }
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
    sourceSets["main"].resources.srcDirs("src/commonMain/moko-resources")

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
        create("MainAppDatabase") {
            packageName.set("app.paralino.core.data.db")
            srcDirs.setFrom("src/commonMain/db/main")
        }
        create("LogsDatabase") {
            packageName.set("app.paralino.core.data.db")
            srcDirs.setFrom("src/commonMain/db/logs")
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
        buildConfigField(FieldSpec.Type.INT, "cryptoScheme", props.getProperty("cryptoScheme").trimQuotes())
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