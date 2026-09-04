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

            // API local.
            //
            // 10.0.2.2 é como o emulador enxerga o "localhost" da máquina —
            // dentro dele, 127.0.0.1 é o próprio emulador. A porta 8010 é a que
            // o Sail publica neste projeto.
            //
            // Em aparelho físico o emulador não ajuda: aí vale o NGROK_URL do
            // local.properties, que tem precedência quando existe.
            val ngrokUrl = localProperties.getProperty("NGROK_URL", "")
            val localApiUrl = if (ngrokUrl.isNotEmpty()) ngrokUrl else "http://10.0.2.2:8010/api"
            buildConfigField("String", "API_BASE_URL", "\"$localApiUrl\"")
            buildConfigField("Boolean", "IS_PRODUCTION", "false")

            // Nome do app diferente para local
            resValue("string", "app_name", "Pixstop Local")

            // A API local fala HTTP puro; o Android bloqueia isso desde a API 28.
            // A permissão fica restrita a este flavor e aos endereços do
            // arquivo network_security_config.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config"
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

            // Staging pode apontar para um túnel HTTP durante o desenvolvimento.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config"
        }

        create("production") {
            dimension = "environment"

            // URL da API de Produção
            buildConfigField("String", "API_BASE_URL", "\"https://pixstop.com.br/api\"")
            buildConfigField("Boolean", "IS_PRODUCTION", "true")

            // Nome do app de produção
            resValue("string", "app_name", "Pixstop")

            // Produção nunca fala HTTP puro.
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config_production"
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
