import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.daemon.common.trimQuotes
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
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
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    androidTarget()

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
            api(libs.koin.core)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
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
            api(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.get().dependsOn(commonMain.get())
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
            implementation(libs.ktor.client.darwin)
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

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
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

    defaultConfigs {}

    targetConfigs {
        create("ios") {
            buildConfigField(FieldSpec.Type.STRING, "glitchTipDSN", props.getProperty("glitchTipDSNiOS").trimQuotes())
        }
    }
}