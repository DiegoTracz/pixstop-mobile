import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.material.icons)  // Material Icons (Android)

            // Ktor - Android Engine
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            // Ktor - iOS Engine
            implementation(libs.ktor.client.darwin)
        }

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Ktor - HTTP Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)

            // Kotlinx Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Multiplatform Settings - Persistência
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.multiplatform.settings.serialization)
            implementation(libs.multiplatform.settings.coroutines)

            // Navigation Compose
            implementation(libs.navigation.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.pixstop.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Flavors devem coincidir com androidApp
    flavorDimensions += "environment"
    productFlavors {
        create("local") {
            dimension = "environment"
        }
        create("staging") {
            dimension = "environment"
        }
        create("production") {
            dimension = "environment"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

// ══════════════════════════════════════════════════════════════════════════
// 🔧 BuildKonfig - Configuração de Ambientes (Staging / Production)
// ══════════════════════════════════════════════════════════════════════════
// O ambiente é detectado AUTOMATICAMENTE pelo Build Variant selecionado:
//   - stagingDebug / stagingRelease     → staging
//   - productionDebug / productionRelease → production
//
// Também pode ser forçado via terminal:
//   ./gradlew :androidApp:assembleStagingDebug -Penvironment=staging
//
// Para dev com ngrok, passe a URL via Gradle property:
//   ./gradlew ... -PapiUrl=https://xxxx.ngrok-free.app/api
// Ou defina NGROK_URL no local.properties (usado automaticamente pelo androidApp)
// ══════════════════════════════════════════════════════════════════════════

// Detecta o ambiente automaticamente pelo nome da task do Gradle
// Quando você seleciona "localDebug" no Android Studio, a task contém "Local"
fun detectEnvironmentFromTask(): String {
    val taskNames = gradle.startParameter.taskRequests
        .flatMap { it.args }
        .map { it.lowercase() }

    return when {
        taskNames.any { it.contains("local") } -> "local"
        taskNames.any { it.contains("staging") } -> "staging"
        taskNames.any { it.contains("production") || it.contains("prod") } -> "production"
        else -> "production" // padrão
    }
}

// Prioridade: 1) -Penvironment=  2) detecção automática pela task  3) production
val environment = project.findProperty("environment")?.toString()
    ?: detectEnvironmentFromTask()

val customApiUrl = project.findProperty("apiUrl")?.toString()

// Lê NGROK_URL do local.properties para o ambiente local
val ngrokUrl: String = try {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.readLines()
            .firstOrNull { it.startsWith("NGROK_URL=") }
            ?.substringAfter("=")
            ?.trim()
            ?: ""
    } else ""
} catch (_: Exception) { "" }

val baseUrl = customApiUrl ?: when (environment) {
    "local" -> if (ngrokUrl.isNotEmpty()) ngrokUrl else "http://10.0.2.2/api"
    "staging" -> "https://staging.pixstop.com.br/api"
    else -> "https://pixstop.com.br/api"
}

val isProduction = environment == "production"

buildkonfig {
    packageName = "com.pixstop.mobile"

    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", baseUrl)
        buildConfigField(STRING, "ENVIRONMENT", environment)
        buildConfigField(BOOLEAN, "IS_PRODUCTION", isProduction.toString())
    }
}
