import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.omnitune.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omnitune.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "0.6.0"

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



    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
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

    lint {
        disable.add("UnsafeOptInUsageError")
        disable.add("IconLauncherShape")
        disable.add("IconLocation")
        disable.add("UseKtx")
        disable.add("UseTomlInstead")
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
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.media3.common.util.UnstableApi"
        )
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
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
    implementation(libs.media3.exoplayer.workmanager)
    implementation(libs.work.runtime)

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



