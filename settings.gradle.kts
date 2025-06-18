pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
        maven { url = uri("https://repo.eclipse.org/content/repositories/paho-releases/") }
    }
}

rootProject.name = "Solution-of-Things"
include(":app")

// Configuración de propiedades del proyecto
gradle.beforeProject { project ->
    project.extensions.extraProperties.set("kotlin.code.style", "official")
    project.extensions.extraProperties.set("android.useAndroidX", true)
    project.extensions.extraProperties.set("android.enableJetifier", true)
}
 