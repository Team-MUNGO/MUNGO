plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.example.poco"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.poco"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.okhttp.bom))
    implementation(libs.bundles.androidx.core)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.bundles.room)
    implementation(libs.bundles.kotlin.extensions)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.network)
    implementation(libs.bundles.hilt)
    implementation(libs.timber)

    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.bundles.unit.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.test)
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.bundles.compose.debug)

    // Pleos Connect
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.text.to.speech)
    implementation(libs.pleos.speech.to.text)
}
