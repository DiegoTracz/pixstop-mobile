import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    kotlin("android")
}

// Lê as propriedades do local.properties
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.pixstop.mobile.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.pixstop.mobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = (project.findProperty("app.versionCode") as String).toInt()
        versionName = project.findProperty("app.versionName") as String
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🔧 Product Flavors - Ambientes Local, Staging e Production
    // ══════════════════════════════════════════════════════════════════════════
    flavorDimensions += "environment"

    productFlavors {
        create("local") {
            dimension = "environment"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"

            // URL da API Local - Usa NGROK_URL do local.properties
            val ngrokUrl = localProperties.getProperty("NGROK_URL", "")
            val localApiUrl = if (ngrokUrl.isNotEmpty()) ngrokUrl else "http://10.0.2.2/api"
            buildConfigField("String", "API_BASE_URL", "\"$localApiUrl\"")
            buildConfigField("Boolean", "IS_PRODUCTION", "false")

            // Nome do app diferente para local
            resValue("string", "app_name", "Pixstop Local")
        }

        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"

            // URL da API de Staging - Usa NGROK_URL do local.properties se disponível
            val ngrokUrl = localProperties.getProperty("NGROK_URL", "")
            val stagingApiUrl = if (ngrokUrl.isNotEmpty()) ngrokUrl else "https://staging.pixstop.com.br/api"
            buildConfigField("String", "API_BASE_URL", "\"$stagingApiUrl\"")
            buildConfigField("Boolean", "IS_PRODUCTION", "false")

            // Nome do app diferente para staging
            resValue("string", "app_name", "Pixstop Staging")
        }

        create("production") {
            dimension = "environment"

            // URL da API de Produção
            buildConfigField("String", "API_BASE_URL", "\"https://pixstop.com.br/api\"")
            buildConfigField("Boolean", "IS_PRODUCTION", "true")

            // Nome do app de produção
            resValue("string", "app_name", "Pixstop")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.koin.android)
    implementation(projects.composeApp)
    implementation(libs.compose.uiTooling)
    implementation(libs.androidx.activity.compose)
}
