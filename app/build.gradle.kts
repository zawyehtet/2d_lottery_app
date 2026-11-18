plugins {
    id("com.android.application")
}

android {
    namespace = "com.lucky.lucky2d"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lucky.lucky2d"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.2"
    }

    signingConfigs {
        getByName("debug") {
            val rootKeystore = rootProject.file("debug.keystore")
            if (rootKeystore.exists()) {
                storeFile = rootKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.activity:activity:1.10.1")
}
