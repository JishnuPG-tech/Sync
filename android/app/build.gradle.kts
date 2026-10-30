plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ghostmesh.app"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("testSigning") {
            storeFile = file("/data/antigravity/test_keystore.jks")
            storePassword = "testpass123"
            keyAlias = "testkey"
            keyPassword = "testpass123"
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("mainApp") {
            dimension = "version"
            applicationId = "com.ghostmesh.app"
            resValue("string", "app_name", "Sync")
        }
        create("peerTwo") {
            dimension = "version"
            applicationId = "com.ghostmesh.app.test"
            resValue("string", "app_name", "Sync Test")
            signingConfig = signingConfigs.getByName("testSigning")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
