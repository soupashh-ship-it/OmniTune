buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.5.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.7")
    }
}

plugins {
    alias(libs.plugins.android.application) apply (false)
    alias(libs.plugins.android.library) apply (false)
    alias(libs.plugins.hilt) apply (false)
    alias(libs.plugins.kotlin.jvm) apply (false)
    alias(libs.plugins.kotlin.ksp) apply (false)
    alias(libs.plugins.kotlin.serialization) apply (false)
    alias(libs.plugins.compose.compiler) apply (false)
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
