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

fun loadUserProperties(rootDir: File): Properties? {
    val propsFile = File(rootDir, "user.properties")
    if (!propsFile.exists()) return null
    return Properties().apply { load(FileInputStream(propsFile)) }
}

// This block runs after configuration is complete and tasks are created
tasks.whenTaskAdded {
    val task = this
    // We look for tasks that assemble or bundle our specific variants
    val variantsToCheck = listOf("playStoreRelease", "playStoreReleaseTest", "playStoreDebug")

    // Find if the current task is assembling or bundling one of our target variants
    val targetVariant = variantsToCheck.find { variant ->
        // we capitalize so it can match the task name
        val adjustedName = variant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        task.name.equals("assemble${adjustedName}", ignoreCase = true) ||
                task.name.equals("bundle${adjustedName}", ignoreCase = true)
    }

    if (targetVariant != null) {
        val taskName = task.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
        val verifyTaskName = "verifyFcmConfigFor${taskName}"

        if (tasks.findByName(verifyTaskName) == null) {
            val verifyTask = tasks.register(verifyTaskName) {
                doLast {
                    // Dynamically construct the path based on the variant name
                    // e.g., "androidApp/src/playStoreRelease/res/values/fcm_config.xml"
                    val fcmConfigFile = rootProject.file("androidApp/src/$targetVariant/res/values/fcm_config.xml")

                    if (!fcmConfigFile.exists()) {
                        throw FileNotFoundException("Missing Firebase config file for $targetVariant at ${fcmConfigFile.path}")
                    }
                }
            }
            task.dependsOn(verifyTask)
        }
    }
}

android {
    namespace = "app.paralino.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    // release config is dynamically injected in release pipeline
    androidComponents.beforeVariants { variant ->
        val names = variant.productFlavors.map { flavor -> flavor.second }
        val variantIgnore = variant.buildType == "releaseTest" && names.contains("direct")
                || variant.buildType == "releaseLocal" && names.contains("playStore")
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
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        base.archivesName = "Paralino_v$versionName($versionCode)"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val userProps = loadUserProperties(rootProject.rootDir)
        resValue("string", "glitchTipDSN", userProps?.getProperty("glitchTipDSNAndroid") ?: "")

        // room schema export
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    // signing configs are dynamically injected in release pipeline
    signingConfigs {
        val userProps = loadUserProperties(rootProject.rootDir)

        apply {
            val storePasswordValue = userProps?.getProperty("testKeyPassword")
            val keyAliasValue = userProps?.getProperty("testKeyAlias")
            val storeFileValue = file("${projectDir.parentFile}/paralinoTestKey")
            if (!storePasswordValue.isNullOrBlank() && !keyAliasValue.isNullOrBlank() && storeFileValue.exists()) {
                create("testConfig") {
                    storePassword = storePasswordValue
                    keyAlias = keyAliasValue
                    keyPassword = storePasswordValue
                    storeFile = storeFileValue
                }
            }
        }

        apply {
            val storePasswordValue = userProps?.getProperty("releaseStorePassword")
            val keyAliasValue = userProps?.getProperty("releaseKeyAlias")
            val keyPasswordValue = userProps?.getProperty("releaseKeyPassword")
            val storeFileValue = file("${projectDir.parentFile}/releaseKeystore.jks")

            if (!storePasswordValue.isNullOrBlank() && !keyAliasValue.isNullOrBlank() && !keyPasswordValue.isNullOrBlank() && storeFileValue.exists()) {
                create("releaseConfig") {
                    storePassword = storePasswordValue
                    keyAlias = keyAliasValue
                    keyPassword = keyPasswordValue
                    storeFile = storeFileValue
                }
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            buildConfigField("String", "devServerUrl", "\"\"")
            buildConfigField("String", "devServerKey", "\"\"")
            buildConfigField("String", "devPublicServerUrl", "\"\"")

            buildConfigField("boolean", "internalTesting", "false")
            buildConfigField("boolean", "forceLogToSystem", "false")

            signingConfigs.findByName("releaseConfig")
                ?.takeIf { it.storeFile != null }
                ?.let { config ->
                    this.signingConfig = config
                }
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

            val userProps = loadUserProperties(rootProject.rootDir)
            buildConfigField("String", "devServerUrl", userProps?.getProperty("devServerUrl") ?: "\"\"")
            buildConfigField("String", "devServerKey", userProps?.getProperty("devServerKey") ?: "\"\"")
            buildConfigField("String", "devPublicServerUrl", userProps?.getProperty("devPublicServerUrl") ?: "\"\"")

            signingConfigs.findByName("testConfig")
                ?.takeIf { it.storeFile != null }
                ?.let { config ->
                    this.signingConfig = config
                }
        }
        create("releaseTest") {
            initWith(getByName("release"))
            matchingFallbacks.add("release")
            buildConfigField("boolean", "internalTesting", "true")
            buildConfigField("boolean", "forceLogToSystem", "true")
            isDebuggable = true

            signingConfigs.findByName("testConfig")
                ?.takeIf { it.storeFile != null }
                ?.let { config ->
                    this.signingConfig = config
                }
        }
        create("releaseLocal") {
            initWith(getByName("release"))
            matchingFallbacks.add("release")
            isDefault = true
            (signingConfigs.findByName("testConfig")?.takeIf { it.storeFile != null }
                ?: signingConfigs.findByName("debug")?.takeIf { it.storeFile != null })
                ?.let { config ->
                    this.signingConfig = config
                }
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("playStore") {
            dimension = "version"
            versionCode = 92
            versionName = "2.4.0"

            val userProps = loadUserProperties(rootProject.rootDir)
            buildConfigField("String", "revenueCatApiKey", userProps?.getProperty("revenueCatApiKey") ?: "null")
            manifestPlaceholders["MAPS_API_KEY"] = userProps?.getProperty("MAPS_API_KEY").orEmpty()
        }
        create("direct") {
            dimension = "version"
            versionCode = 92
            versionName = "2.4.0"
            isDefault = true

            buildConfigField("String", "revenueCatApiKey", "null")
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

    implementation(libs.nanoid)
    implementation(libs.sentry.android)

    implementation(project(path = ":parasodium"))
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

// used by UnifiedPush
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