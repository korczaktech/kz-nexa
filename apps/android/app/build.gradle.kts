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
        val commitCount = providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
        versionCode = commitCount
        versionName = "0.0.0.$commitCount"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
