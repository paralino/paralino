import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.daemon.common.trimQuotes
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries)
    // have to use id here since build will fail otherwise because of duplicate versions
    // check later id fixed
    // https://github.com/gradle/gradle/issues/17968
    id(libs.plugins.kotlin.parcelize.get().pluginId)
}

android {
    namespace = "app.paralino.android"
    compileSdk = 34

    androidComponents.beforeVariants { variant ->
        val names = variant.productFlavors.map { flavor -> flavor.second }
        val variantIgnore = (variant.buildType == "debug" && names.contains("playStore")) ||
                (variant.buildType == "debug" && names.contains("playStoreTest")) ||
                (variant.buildType == "debug" && names.contains("direct")) ||
                (variant.buildType == "release" && names.contains("dev")) ||
                (names.contains("playStore") && names.contains("oss"))
        variant.enable = !variantIgnore
    }

    applicationVariants.all {
        outputs.all {
            val name = "Paralino"
            val sep = "_"
            val flavor = productFlavors[0].name.take(3)
            val buildType = buildType.name.take(1).uppercase()
            val versionName = versionName
            val newApkName = "$name$sep" + "v$versionName$buildType.apk"
            (this as BaseVariantOutputImpl).outputFileName = newApkName
        }
    }

    defaultConfig {
        applicationId = "app.paralino.android"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        setProperty("archivesBaseName", "Paralino_v$versionName($versionCode)")

        buildConfigField("boolean", "internalTesting", "false")
        buildConfigField("boolean", "forceLogToSystem", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val propsFile = rootProject.file("user.properties")
        if (!propsFile.exists()) {
            throw FileNotFoundException("Missing properties config file")
        }

        val props = Properties()
        props.load(FileInputStream(propsFile))
        manifestPlaceholders["MAPS_API_KEY"] = props["MAPS_API_KEY"] as String
        resValue("string", "glitchTipDSN", props["glitchTipDSNAndroid"] as String)

        buildConfigField("String", "serverUrl", props["productionServerUrl"] as String)
        buildConfigField("String", "serverKey", props["productionServerKey"] as String)
        buildConfigField("String", "publicServerUrl", props["productionPublicServerUrl"] as String)

        // room schema export
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    signingConfigs {
        create("testConfig") {
            try {
                val propsFile = rootProject.file("user.properties")
                val props = Properties()
                props.load(FileInputStream(propsFile))
                storePassword = props["testKeyPassword"] as String
                keyAlias = props["testKeyAlias"] as String
                keyPassword = props["testKeyPassword"] as String
                storeFile = file("${projectDir.parentFile}/paralinoTestKey")
            } catch (ex: Exception) {
                throw ex
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            versionNameSuffix = "D"
            signingConfig = signingConfigs.getByName("testConfig")
        }
    }

    flavorDimensions += "version"
    flavorDimensions += "source"
    productFlavors {
        create("playStore") {
            dimension = "version"
            versionCode = 26
            versionName = "0.9.5"

            val fcmConfigFile = rootProject.file("androidApp/src/playStore/res/values/fcm_config.xml")
            if (!fcmConfigFile.exists()) {
                throw FileNotFoundException("Missing Firebase config file")
            }
        }
        create("direct") {
            dimension = "version"
            versionCode = 20
            versionName = "0.8.3"
            versionNameSuffix = "-int"
            applicationIdSuffix = ".int"

            buildConfigField("boolean", "forceLogToSystem", "false")

            signingConfig = signingConfigs.getByName("testConfig")
        }
        create("dev") {
            dimension = "version"
            versionCode = 21
            versionName = "0.8.4"
            versionNameSuffix = "-dev"
            applicationIdSuffix = ".dev"

            buildConfigField("boolean", "internalTesting", "true")
            buildConfigField("boolean", "forceLogToSystem", "true")

            val propsFile = rootProject.file("user.properties")
            val props = Properties()
            props.load(FileInputStream(propsFile))
            buildConfigField("String", "serverUrl", props["devServerUrl"] as String)
            buildConfigField("String", "serverKey", props["devServerKey"] as String)
            buildConfigField("String", "publicServerUrl", props["devPublicServerUrl"] as String)

            signingConfig = signingConfigs.getByName("testConfig")
        }
        create("oss") {
            dimension = "source"
            val propsFile = rootProject.file("user.properties")
            if (propsFile.exists()) {
                val props = Properties()
                props.load(FileInputStream(propsFile))
                buildConfigField("String", "mapboxAccessToken", props["MAPBOX_ACCESS_TOKEN"] as String)
            }
        }
        create("google") {
            dimension = "source"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // from coroutines
        resources.excludes += "DebugProbesKt.bin"
    }
}

aboutLibraries {
    configPath = "config"
    additionalLicenses = arrayOf("svg_backgrounds")
    prettyPrint = true
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

repositories {
    maven {
        url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
        authentication {
            create<BasicAuthentication>("basic")
        }
        val props = Properties()
        props.load(FileInputStream(rootProject.file("user.properties")))
        credentials {
            username = "mapbox"
            password = props["MAPBOX_SECRET_TOKEN"] as String
        }
    }
}

val ossImplementation by configurations
val googleImplementation by configurations
dependencies {
    implementation(project(path = ":shared"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)

    // region android and kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.legacy.support.core.utils)

    implementation(libs.work.runtime.ktx)
    implementation(libs.lifecycle.common.java8)
    implementation(libs.lifecycle.extensions)
    implementation(libs.lifecycle.runtime.ktx)
    // endregion

    // region compose
    implementation(libs.constraintlayout.compose)
    implementation(libs.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    // implementation(libs.compose.material.icons.core)
    // implementation(libs.compose.material.icons.extended)

    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)
    // endregion

    // region di
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    // endregion

    // region utils
    implementation(libs.play.services.location)
    googleImplementation(libs.firebase.messaging.ktx)

    ossImplementation(libs.osmdroid.android)
    googleImplementation(libs.maps.compose)
    googleImplementation(libs.maps.compose.utils)
    googleImplementation(libs.maps.compose.widgets)
    ossImplementation(libs.mapbox.android)
    ossImplementation(libs.mapbox.sdk.turf)

    implementation(project(path = ":lazysodium-android"))
    implementation(libs.jna) {
        artifact { type = "aar" }
    }
    // implementation("net.java.dev.jna:jna:5.14.0@aar")
    implementation(libs.nanoid)

    implementation(libs.aboutLibraries.core)
    implementation(libs.aboutLibraries.compose)
    implementation(libs.sentry.android)

    implementation(project(path = ":appwrite-android"))
//    releaseImplementation(libs.appwrite.android)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    debugImplementation(libs.logging.interceptor)

    implementation(libs.play.billing)
    // endregion
}
