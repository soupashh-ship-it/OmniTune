plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)
    implementation(libs.newpipe.extractor)
    implementation(libs.re2j)
    implementation(libs.rhino)
    testImplementation(libs.junit)
}

val includeLiveNetworkTests =
    providers.gradleProperty("includeLiveNetworkTests").orElse("false").get().toBoolean()

tasks.withType<Test>().configureEach {
    useJUnit {
        if (!includeLiveNetworkTests) {
            excludeCategories("com.omnitune.innertube.LiveNetwork")
        }
    }
}
