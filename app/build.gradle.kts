import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.omnitune.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omnitune.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val localProperties = project.rootProject.file("local.properties")
            .takeIf { it.exists() }
            ?.let { Properties().apply { load(it.inputStream()) } }

        val lastfmApiKey = localProperties?.getProperty("LASTFM_API_KEY")
            ?: System.getenv("LASTFM_API_KEY")
            ?: ""
        val lastfmSecret = localProperties?.getProperty("LASTFM_SECRET")
            ?: System.getenv("LASTFM_SECRET")
            ?: ""

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastfmApiKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastfmSecret\"")

        val togetherBearerToken = localProperties?.getProperty("TOGETHER_BEARER_TOKEN")
            ?: System.getenv("TOGETHER_BEARER_TOKEN") ?: ""
        buildConfigField("String", "TOGETHER_BEARER_TOKEN", "\"$togetherBearerToken\"")
    }

    flavorDimensions += "abi"
    productFlavors {
        create("universal") {
            dimension = "abi"
            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") }
            buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        }
        create("arm64") {
            dimension = "abi"
            ndk { abiFilters += "arm64-v8a" }
            buildConfigField("String", "ARCHITECTURE", "\"arm64\"")
        }
        create("armeabi") {
            dimension = "abi"
            ndk { abiFilters += "armeabi-v7a" }
            buildConfigField("String", "ARCHITECTURE", "\"armeabi\"")
        }
        create("x86") {
            dimension = "abi"
            ndk { abiFilters += "x86" }
            buildConfigField("String", "ARCHITECTURE", "\"x86\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
            "/META-INF/LICENSE",
            "/META-INF/LICENSE.txt",
            "/META-INF/NOTICE",
            "/META-INF/NOTICE.txt",
            "/META-INF/*.md"
        )
    }
}

kotlin {
    jvmToolchain(21)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-Xannotation-default-target=param-property",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

dependencies {
    implementation("androidx.palette:palette-ktx:1.0.0")
    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)

    implementation(libs.activity)
    implementation(libs.navigation)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    implementation(libs.material3)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.okhttp)
    implementation(libs.media3.session)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.timber)
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)
    implementation(libs.datastore)
    implementation(libs.annotation)
    implementation(libs.apache.lang3)
    implementation(libs.kuromoji.ipadic)

    implementation(project(":innertube"))
    implementation(project(":simpmusic"))
    implementation(project(":betterlyrics"))
    implementation(project(":lrclib"))
    implementation(project(":kugou"))
    implementation(project(":lastfm"))
    implementation(project(":kizzy"))
    implementation(project(":canvas"))

    testImplementation(libs.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}



