plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    viewBinding {
        enable = true
    }
    dataBinding {
        enable = true
    }
    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
    buildToolsVersion = "35.0.0"
    ndkVersion = "29.0.13113456 rc1"
}

dependencies {
    //mqtt
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    // Para Android (necesita el Service adicional)
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    // Para que Paho encuentre LocalBroadcastManager
    implementation ("androidx.localbroadcastmanager:localbroadcastmanager:1.0.0")
// Y el bridge legacy (por si acaso)
    implementation ("androidx.legacy:legacy-support-v4:1.0.0")

    //fin

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.media3.common.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}