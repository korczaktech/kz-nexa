plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.korczaktech.nexa"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.korczaktech.nexa"
        minSdk = 26
        targetSdk = 35
        buildFeatures {
            buildConfig = true
        }
        val commitsAfterReset = providers.exec {
            commandLine("git", "rev-list", "--count", "6b3c0a21d3ac28b34378d9b4452e966c67dd8f72..HEAD")
        }.standardOutput.asText.get().trim().toIntOrNull() ?: 0
        val nexVersion = (commitsAfterReset - 2).coerceAtLeast(2)
        val legacySafeVersionCode = providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim().toIntOrNull()?.coerceAtLeast(1) ?: nexVersion
        versionCode = legacySafeVersionCode
        versionName = "0.0.0.$nexVersion"
    }
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("NEXA_KEYSTORE_FILE")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("NEXA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NEXA_KEY_ALIAS")
                keyPassword = System.getenv("NEXA_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies {
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}