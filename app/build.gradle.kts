import java.util.Properties

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
    // 차량 속도 등 Android Automotive 표준 차량 속성 (Pleos SDK에 없는 값)
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.example.poco"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val assistantUrl = providers.gradleProperty("POCO_ASSISTANT_URL").orElse("").get()
        require(assistantUrl.isEmpty() || assistantUrl.matches(Regex("https://[A-Za-z0-9./_-]+"))) {
            "POCO_ASSISTANT_URL must be an HTTPS function URL without query parameters"
        }
        buildConfigField("String", "ASSISTANT_URL", "\"$assistantUrl\"")

        // Public Firebase Web API key (project identifier, not a secret) for anonymous auth.
        // Empty = development invitation-code mode. Gemini key never enters the APK.
        val localText: String = providers.fileContents(
            rootProject.layout.projectDirectory.file("local.properties")
        ).asText.orElse("").get()
        val localApiKey: String = Regex("(?m)^POCO_FIREBASE_API_KEY=(\\S*)$")
            .find(localText)?.groupValues?.get(1) ?: ""
        val firebaseApiKey: String = providers.gradleProperty("POCO_FIREBASE_API_KEY")
            .orElse(localApiKey).get()
        require(
            firebaseApiKey.isEmpty() || firebaseApiKey.matches(Regex("AIza[0-9A-Za-z_-]{35}"))
        ) {
            "POCO_FIREBASE_API_KEY must be a Firebase Web API key"
        }
        buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")
    }

    // Release 서명: 루트의 keystore.properties(커밋 금지)에서 읽는다. 없으면 서명 없이 빌드된다.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
    implementation(libs.pleos.fused.location)
    implementation(libs.pleos.navi.helper)
}
