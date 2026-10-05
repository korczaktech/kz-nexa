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
            commandLine("git", "rev-list", "--count", "0.0.0.652..HEAD")
        }.standardOutput.asText.get().trim().toIntOrNull() ?: 0
        val nexVersion = (commitsAfterReset - 2).coerceAtLeast(2)
        versionCode = nexVersion
        versionName = "0.0.0.$nexVersion"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
