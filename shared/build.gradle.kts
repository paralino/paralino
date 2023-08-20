plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight")
    id("dev.icerock.mobile.multiplatform-resources")
    id("org.jetbrains.compose")
}

kotlin {
    android()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0.0"
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        ios.deploymentTarget = "16.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
            linkerOpts.add("-lsqlite3")
        }
        extraSpecAttributes["resources"] = "['src/commonMain/resources/**', 'src/iosMain/resources/**']"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                api("app.cash.sqldelight:coroutines-extensions:2.0.0-rc02")
                api("app.cash.sqldelight:primitive-adapters:2.0.0-rc02")
                implementation("com.russhwolf:multiplatform-settings-coroutines:1.0.0")
                implementation("com.russhwolf:multiplatform-settings:1.0.0")
                api("io.insert-koin:koin-core:3.2.0")
                api("dev.icerock.moko:resources:0.23.0")
                api("dev.icerock.moko:resources-compose:0.23.0")
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
            }
        }

        val androidMain by getting {
            dependencies {
                api("io.insert-koin:koin-android:3.2.0")
                api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
                implementation("androidx.preference:preference-ktx:1.2.0")
                implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
                implementation("app.cash.sqldelight:android-driver:2.0.0-rc02")
                api("androidx.activity:activity-compose:1.7.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")
            }
        }
//        val androidTest by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.0-rc02")
            }
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

// Workaround for compose multiplatform not using latest kotlin version
// this silences the error and it should be fine for minor versions
// sqldelight uses 1.8.22 which causes build problems
// remove after compose catches up
compose {
    kotlinCompilerPlugin.set(dependencies.compiler.forKotlin("1.8.20"))
    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.8.22")
}

android {
    namespace = "app.paralino"
    compileSdk = 33

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

multiplatformResources {
    multiplatformResourcesPackage = "app.paralino.core"
    multiplatformResourcesClassName = "MR"
    multiplatformResourcesVisibility = dev.icerock.gradle.MRVisibility.Public
    iosBaseLocalizationRegion = "en"
    multiplatformResourcesSourceSet = "commonMain"
    disableStaticFrameworkWarning = true
}