// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

// Configuración global del proyecto
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
    }
}

// Configuración de dependencias globales
subprojects {
    configurations.all {
        resolutionStrategy {
            // Forzar versiones específicas para evitar conflictos
            force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
        }
    }
}

// Configuración de tareas globales
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Configuración de propiedades del proyecto
ext {
    set("compileSdkVersion", 35)
    set("targetSdkVersion", 35)
    set("minSdkVersion", 24)
    set("buildToolsVersion", "34.0.0")
    
    // Versiones de dependencias principales
    set("kotlinVersion", "2.0.21")
    set("agpVersion", "8.7.3")
    set("coreKtxVersion", "1.16.0")
    set("appcompatVersion", "1.7.0")
    set("materialVersion", "1.12.0")
    set("retrofitVersion", "2.11.0")
    set("coroutinesVersion", "1.7.3")
    set("roomVersion", "2.6.1")
    set("navigationVersion", "2.7.7")
    set("lifecycleVersion", "2.7.0")
    set("workVersion", "2.9.0")
    set("biometricVersion", "1.1.0")
    set("securityVersion", "1.1.0-alpha06")
    set("preferenceVersion", "1.2.1")
    set("multidexVersion", "2.0.1")
    
    // Versiones de testing
    set("junitVersion", "4.13.2")
    set("androidxTestVersion", "1.1.5")
    set("espressoVersion", "3.5.1")
    set("mockitoVersion", "5.8.0")
    set("mockitoKotlinVersion", "5.2.1")
    
    // Versiones de herramientas de desarrollo
    set("leakcanaryVersion", "2.12")
    set("glideVersion", "4.16.0")
    set("okhttpVersion", "4.12.0")
    set("gsonVersion", "2.10.1")
    
    // Versiones de Google Play Services
    set("playServicesAdsVersion", "22.6.0")
    set("playServicesLocationVersion", "21.1.0")
    
    // Versiones de MQTT
    set("pahoMqttVersion", "1.2.5")
    set("pahoAndroidServiceVersion", "1.1.1")
    set("hivemqMqttVersion", "1.3.3")
}