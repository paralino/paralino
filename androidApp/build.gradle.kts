import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "app.paralino.android"
    compileSdk = 36

    androidComponents.beforeVariants { variant ->
        val names = variant.productFlavors.map { flavor -> flavor.second }
        val variantIgnore = (variant.buildType == "releaseTest" && names.contains("direct"))
        variant.enable = !variantIgnore

        if ((variant.buildType == "release" && names.contains("playStore"))) {
            val fcmConfigFile = rootProject.file("androidApp/src/playStoreRelease/res/values/fcm_config.xml")
            if (!fcmConfigFile.exists()) {
                throw FileNotFoundException("Missing Firebase config file for PRODUCTION_RELEASE")
            }
        } else if ((variant.buildType == "releaseTest" && names.contains("playStore"))) {
            val fcmConfigFile = rootProject.file("androidApp/src/playStoreReleaseTest/res/values/fcm_config.xml")
            if (!fcmConfigFile.exists()) {
                throw FileNotFoundException("Missing Firebase config file for TEST_RELEASE")
            }
        } else if ((variant.buildType == "debug" && names.contains("playStore"))) {
            val fcmConfigFile = rootProject.file("androidApp/src/playStoreDebug/res/values/fcm_config.xml")
            if (!fcmConfigFile.exists()) {
                throw FileNotFoundException("Missing Firebase config file for DEBUG")
            }
        }
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
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        base.archivesName = "Paralino_v$versionName($versionCode)"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val propsFile = rootProject.file("user.properties")
        val props = Properties()
        props.load(FileInputStream(propsFile))

        manifestPlaceholders["MAPS_API_KEY"] = props["MAPS_API_KEY"] as String
        resValue("string", "glitchTipDSN", props["glitchTipDSNAndroid"] as String)

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
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            buildConfigField("boolean", "internalTesting", "false")
            buildConfigField("boolean", "forceLogToSystem", "false")

            val propsFile = rootProject.file("user.properties")
            val props = Properties()
            props.load(FileInputStream(propsFile))
            buildConfigField("String", "serverUrl", props["productionServerUrl"] as String)
            buildConfigField("String", "serverKey", props["productionServerKey"] as String)
            buildConfigField("String", "publicServerUrl", props["productionPublicServerUrl"] as String)
        }
        getByName("debug") {
            isDebuggable = true
//            isMinifyEnabled = true
//            isShrinkResources = true
//            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            versionNameSuffix = "D"
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
        create("releaseTest") {
            initWith(getByName("release"))
            matchingFallbacks.add("release")
            buildConfigField("boolean", "internalTesting", "true")
            buildConfigField("boolean", "forceLogToSystem", "true")
            isDebuggable = true
            signingConfig = signingConfigs.getByName("testConfig")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("playStore") {
            dimension = "version"
            versionCode = 79
            versionName = "2.2.2"

            val propsFile = rootProject.file("user.properties")
            val props = Properties()
            props.load(FileInputStream(propsFile))
            buildConfigField("String", "revenueCatApiKey", props["revenueCatApiKey"] as String)

            buildConfigField("boolean", "hasGoogleMaps", "true")
            buildConfigField("boolean", "hasNotificationSocketService", "false")
        }
        create("direct") {
            dimension = "version"
            versionCode = 79
            versionName = "2.2.2"

            buildConfigField("String", "revenueCatApiKey", "\"\"")
            buildConfigField("boolean", "hasGoogleMaps", "false")
            buildConfigField("boolean", "hasNotificationSocketService", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    androidResources {
        generateLocaleConfig = true
        localeFilters.addAll(
            listOf(
                "en",  // base/English
                "bs",  // Bosnian
                "ca",  // Catalan
                "cs",  // Czech
                "da",  // Danish
                "de",  // German
                "el",  // Greek
                "es",  // Spanish
                "fi",  // Finnish
                "fr",  // French
                "hr",  // Croatian
                "hu",  // Hungarian
                "it",  // Italian
                "nb",  // Norwegian Bokmål
                "nl",  // Dutch
                "pl",  // Polish
                "pt",  // Portuguese
                "ro",  // Romanian
                "ru",  // Russian
                "sk",  // Slovak
                "sl",  // Slovenian
                "sr",  // Serbian
                "sv",  // Swedish
                "tr",  // Turkish
                "uk"   // Ukrainian
            )
        )
    }
}

val directImplementation by configurations
val playStoreImplementation by configurations
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

    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)
    // endregion

    // region di
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.navigation)
    // endregion

    // region utils
    playStoreImplementation(libs.play.services.location)
    playStoreImplementation(libs.firebase.messaging.ktx)
    playStoreImplementation(libs.play.review)

    playStoreImplementation(libs.revenuecat.android)

    playStoreImplementation(libs.maps.google.compose)
    playStoreImplementation(libs.maps.google.compose.utils)
    implementation(libs.maps.maplibre.compose)
    directImplementation(libs.unifiedPush.connector)

    implementation(project(path = ":lazysodium-android"))
    implementation(libs.jna) {
        artifact { type = "aar" }
    }
    implementation(libs.nanoid)
    implementation(libs.sentry.android)

    implementation(project(path = ":appwrite-android"))
//    releaseImplementation(libs.appwrite.android)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    debugImplementation(libs.logging.interceptor)

    implementation(libs.network.ktor.client.core)
    implementation(libs.network.ktor.client.logging)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    // endregion
}

configurations.all {
    val tink = "com.google.crypto.tink:tink-android:1.20.0"
    // You can also use the library declaration catalog
    // val tink = libs.google.tink
    resolutionStrategy {
        force(tink)
        dependencySubstitution {
            substitute(module("com.google.crypto.tink:tink")).using(module(tink))
        }
    }
}