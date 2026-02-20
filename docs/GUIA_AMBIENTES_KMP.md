# 📋 Guia para Replicar Ambientes em Outro Projeto KMP

> Passo a passo para adicionar a mesma estrutura de ambientes (local / staging / produção) em qualquer projeto Kotlin Multiplatform.
> Substitua os valores marcados com `← ALTERE` pelos do seu projeto.

---

## Checklist

- [ ] **1.** Adicionar plugin BuildKonfig no `gradle/libs.versions.toml`
- [ ] **2.** Registrar plugin no `build.gradle.kts` raiz
- [ ] **3.** Aplicar plugin e configurar no `composeApp/build.gradle.kts`
- [ ] **4.** Adicionar Product Flavors no `composeApp/build.gradle.kts` (seção `android {}`)
- [ ] **5.** Adicionar Product Flavors no `androidApp/build.gradle.kts`
- [ ] **6.** Refatorar `ApiConfig.kt` para usar `BuildKonfig`
- [ ] **7.** *(Opcional)* Criar `xcconfig` para iOS

---

## Pré-requisito — Configurar URL do Ngrok

Antes de tudo, adicione a URL do ngrok no `local.properties` na raiz do projeto:

```properties
NGROK_URL=https://sua-url-real.ngrok-free.app/api
```

Essa URL é lida automaticamente pelo flavor `local` quando você seleciona `localDebug` no Android Studio.

> ⚠️ O `local.properties` **não** deve ser commitado no Git (já está no `.gitignore`).
> Cada desenvolvedor configura a sua própria URL.

---

## PASSO 1 — `gradle/libs.versions.toml`

```toml
[versions]
# ... versões existentes ...
buildkonfig = "0.15.2"                                          # ← VERSÃO DO BUILDKONFIG

[plugins]
# ... plugins existentes ...
buildkonfig = { id = "com.codingfeline.buildkonfig", version.ref = "buildkonfig" }
```

---

## PASSO 2 — `build.gradle.kts` (raiz)

```kotlin
plugins {
    // ... plugins existentes ...
    alias(libs.plugins.buildkonfig) apply false
}
```

---

## PASSO 3 — `composeApp/build.gradle.kts`

### 3a. Imports e plugin

```kotlin
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN

plugins {
    // ... plugins existentes ...
    alias(libs.plugins.buildkonfig)
}
```

### 3b. Product Flavors (na seção `android {}`)

Os flavors no módulo library **devem coincidir** com os do `androidApp`:

```kotlin
android {
    // ... config existente ...

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
}
```

### 3c. Bloco BuildKonfig (no final do arquivo)

```kotlin
// Detecta o ambiente automaticamente pelo Build Variant selecionado no Android Studio
fun detectEnvironmentFromTask(): String {
    val taskNames = gradle.startParameter.taskRequests
        .flatMap { it.args }
        .map { it.lowercase() }

    return when {
        taskNames.any { it.contains("local") } -> "local"
        taskNames.any { it.contains("staging") } -> "staging"
        taskNames.any { it.contains("production") || it.contains("prod") } -> "production"
        else -> "production"
    }
}

val environment = project.findProperty("environment")?.toString()
    ?: detectEnvironmentFromTask()

val customApiUrl = project.findProperty("apiUrl")?.toString()

// Lê NGROK_URL do local.properties
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
    "local" -> if (ngrokUrl.isNotEmpty()) ngrokUrl else "http://10.0.2.2/api"  // ← ALTERE: fallback local
    "staging" -> "https://staging.seudominio.com/api"                          // ← ALTERE: URL staging
    else -> "https://app.seudominio.com/api"                                   // ← ALTERE: URL produção
}

val isProduction = environment == "production"

buildkonfig {
    packageName = "com.seupackage.app"                                         // ← ALTERE: package

    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", baseUrl)
        buildConfigField(STRING, "ENVIRONMENT", environment)
        buildConfigField(BOOLEAN, "IS_PRODUCTION", isProduction.toString())
    }
}
```

---

## PASSO 4 — `androidApp/build.gradle.kts`

```kotlin
import java.util.Properties

// Lê as propriedades do local.properties
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    // ... defaultConfig existente ...

    flavorDimensions += "environment"

    productFlavors {
        create("local") {
            dimension = "environment"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"

            val ngrokUrl = localProperties.getProperty("NGROK_URL", "")
            val localApiUrl = if (ngrokUrl.isNotEmpty()) ngrokUrl else "http://10.0.2.2/api"  // ← ALTERE
            buildConfigField("String", "API_BASE_URL", "\"$localApiUrl\"")
            buildConfigField("Boolean", "IS_PRODUCTION", "false")

            resValue("string", "app_name", "MeuApp Local")     // ← ALTERE
        }

        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"

            val ngrokUrl = localProperties.getProperty("NGROK_URL", "")
            val stagingApiUrl = if (ngrokUrl.isNotEmpty()) ngrokUrl else "https://staging.seudominio.com/api"  // ← ALTERE
            buildConfigField("String", "API_BASE_URL", "\"$stagingApiUrl\"")
            buildConfigField("Boolean", "IS_PRODUCTION", "false")

            resValue("string", "app_name", "MeuApp Staging")   // ← ALTERE
        }

        create("production") {
            dimension = "environment"

            buildConfigField("String", "API_BASE_URL", "\"https://app.seudominio.com/api\"")  // ← ALTERE
            buildConfigField("Boolean", "IS_PRODUCTION", "true")

            resValue("string", "app_name", "MeuApp")           // ← ALTERE
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

---

## PASSO 5 — `ApiConfig.kt` (commonMain)

```kotlin
package com.seupackage.app.core.config                          // ← ALTERE

import com.seupackage.app.BuildKonfig                           // ← ALTERE

object ApiConfig {

    enum class Environment {
        LOCAL, STAGING, PRODUCTION;
        val isLocal: Boolean get() = this == LOCAL
        val isStaging: Boolean get() = this == STAGING
        val isProduction: Boolean get() = this == PRODUCTION
    }

    val currentEnvironment: Environment = when (BuildKonfig.ENVIRONMENT) {
        "local" -> Environment.LOCAL
        "staging" -> Environment.STAGING
        else -> Environment.PRODUCTION
    }

    val isProduction: Boolean get() = BuildKonfig.IS_PRODUCTION
    val isStaging: Boolean get() = currentEnvironment == Environment.STAGING
    val isLocal: Boolean get() = currentEnvironment == Environment.LOCAL

    var baseUrl: String = BuildKonfig.BASE_URL
        private set

    object Endpoints {
        const val LOGIN = "auth/login"                          // ← ALTERE
        const val LOGOUT = "auth/logout"
        const val PROFILE = "me"
    }

    const val CONNECTION_TIMEOUT_MS = 30_000L
    const val REQUEST_TIMEOUT_MS = 30_000L

    fun configure(url: String) { baseUrl = url.trimEnd('/') }
    fun reset() { baseUrl = BuildKonfig.BASE_URL }
}
```

---

## PASSO 6 *(Opcional)* — xcconfig para iOS

Crie em `iosApp/Configuration/`:

**Config-Staging.xcconfig:**
```
TEAM_ID=
PRODUCT_NAME=MeuApp Staging                                     // ← ALTERE
PRODUCT_BUNDLE_IDENTIFIER=com.seupackage.app.staging$(TEAM_ID)  // ← ALTERE
CURRENT_PROJECT_VERSION=1
MARKETING_VERSION=1.0
ENVIRONMENT=staging
```

**Config-Production.xcconfig:**
```
TEAM_ID=
PRODUCT_NAME=MeuApp                                             // ← ALTERE
PRODUCT_BUNDLE_IDENTIFIER=com.seupackage.app$(TEAM_ID)          // ← ALTERE
CURRENT_PROJECT_VERSION=1
MARKETING_VERSION=1.0
ENVIRONMENT=production
```

---

## 🔀 Comparação: pixstop-mobile vs mobile-immo

| Recurso                          | mobile-immo                      | pixstop-mobile                   |
|----------------------------------|----------------------------------|----------------------------------|
| Product Flavors                  | `staging` / `prod`               | `local` / `staging` / `production` |
| `BuildConfig.API_BASE_URL`       | ✅                                | ✅                                |
| `BuildConfig.IS_PRODUCTION`      | ✅                                | ✅                                |
| `NGROK_URL` em `local.properties`| ✅                                | ✅                                |
| `resValue("app_name")`           | ✅                                | ✅                                |
| `applicationIdSuffix`            | ✅ `.staging`                     | ✅ `.local` / `.staging`          |
| `buildConfig = true`             | ✅                                | ✅                                |
| **BuildKonfig (KMP shared)**     | ❌ URLs hardcoded no ApiConfig    | ✅ Gerado em compile-time         |
| **Detecção automática de flavor**| ❌                                | ✅ `detectEnvironmentFromTask()`  |


