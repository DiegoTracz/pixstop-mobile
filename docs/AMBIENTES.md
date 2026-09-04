# 🔧 Configuração de Ambientes (Local / Staging / Produção)

O projeto utiliza o plugin **[BuildKonfig](https://github.com/yshrsmz/BuildKonfig)** para gerar constantes de configuração em tempo de compilação, permitindo URLs e variáveis diferentes por ambiente — compartilhadas entre Android e iOS.

---

## 📊 Resumo dos Ambientes

| Propriedade                     | Local                               | Staging                              | Produção                         |
|---------------------------------|--------------------------------------|--------------------------------------|----------------------------------|
| **URL da API**                  | `http://10.0.2.2:8010/api`           | `https://staging.pixstop.com.br/api` | `https://pixstop.com.br/api`     |
| **Application ID (Android)**    | `com.pixstop.mobile.local`           | `com.pixstop.mobile.staging`         | `com.pixstop.mobile`             |
| **Bundle ID (iOS)**             | `com.pixstop.mobile.local`           | `com.pixstop.mobile.staging`         | `com.pixstop.mobile`             |
| **Nome do App**                 | Pixstop Local                        | Pixstop Staging                      | Pixstop                          |
| **`BuildKonfig.IS_PRODUCTION`** | `false`                              | `false`                              | `true`                           |
| **`BuildConfig.IS_PRODUCTION`** | `false`                              | `false`                              | `true`                           |

> ⚠️ IDs diferentes = as 3 versões podem ser instaladas no mesmo dispositivo simultaneamente.

---

## 🔌 Apontando para o backend local

O backend roda em Sail, publicado na porta **8010** da sua máquina.

### Emulador

Nada a configurar. `10.0.2.2` é como o emulador enxerga o `localhost` da
máquina — dentro dele, `127.0.0.1` seria o próprio emulador. O flavor `local`
já aponta para `http://10.0.2.2:8010/api`.

Confira que o Sail está no ar antes de abrir o app:

```bash
curl -s http://127.0.0.1:8010/api/config | head -c 80
```

### Aparelho físico

O aparelho não alcança `10.0.2.2`. Duas saídas, na ordem de preferência:

1. **Mesma rede local** — descubra o IP da máquina (`hostname -I`) e ponha no
   `local.properties`:

   ```properties
   NGROK_URL=http://192.168.0.42:8010/api
   ```

2. **Túnel** — quando o aparelho está em outra rede:

   ```properties
   NGROK_URL=https://xxxx.ngrok-free.app/api
   ```

`NGROK_URL` tem precedência sobre o padrão sempre que existir, apesar do nome:
ele serve para qualquer URL de desenvolvimento, não só para o ngrok.

### Por que o HTTP puro funciona só aqui

Desde a API 28 o Android bloqueia tráfego sem TLS. Os flavors `local` e
`staging` liberam a exceção, e mesmo assim só para os endereços listados em
`androidApp/src/main/res/xml/network_security_config.xml`. Em **produção** o
arquivo é outro e não abre exceção nenhuma — nem por engano.

---

## 🏗️ Como usar

### Android Studio

Basta selecionar o **Build Variant** no painel lateral — o ambiente é detectado **automaticamente**:

| Build Variant         | Ambiente   | Tipo    | URL gerada                             |
|-----------------------|------------|---------|----------------------------------------|
| `localDebug`          | Local      | Debug   | `http://10.0.2.2:8010/api`             |
| `localRelease`        | Local      | Release | `http://10.0.2.2:8010/api`             |
| `stagingDebug`        | Staging    | Debug   | `https://staging.pixstop.com.br/api`   |
| `stagingRelease`      | Staging    | Release | `https://staging.pixstop.com.br/api`   |
| `productionDebug`     | Produção   | Debug   | `https://pixstop.com.br/api`           |
| `productionRelease`   | Produção   | Release | `https://pixstop.com.br/api`           |

> ✅ **Não é necessário** passar `-Penvironment=` manualmente. A detecção é automática pelo nome da task.

### Terminal

```shell
# Local (usa NGROK_URL do local.properties)
./gradlew :androidApp:assembleLocalDebug

# Staging
./gradlew :androidApp:assembleStagingDebug

# Produção
./gradlew :androidApp:assembleProductionRelease

# Override temporário de URL
./gradlew :androidApp:assembleLocalDebug -PapiUrl=https://outra-url.ngrok-free.app/api
```

### iOS — Via Xcode

O ambiente é controlado pela variável `APP_ENVIRONMENT` no Build Settings:

1. No Xcode, selecione o **target `iosApp`** no painel lateral
2. Vá em **Build Settings** → procure por `APP_ENVIRONMENT`
3. Altere o valor:

| Valor | Ambiente | URL usada |
|---|---|---|
| `local` | Local (padrão Debug) | `NGROK_URL` do `local.properties` |
| `staging` | Staging | `https://staging.pixstop.com.br/api` |
| `production` | Produção (padrão Release) | `https://pixstop.com.br/api` |

> ✅ **Debug** usa `local` por padrão. **Release** usa `production` por padrão.

### iOS — Via terminal

```shell
# Local
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -Penvironment=local

# Staging
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -Penvironment=staging

# Produção
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -Penvironment=production
```

---

## 🌐 Configurar URL do Ngrok (dev local)

Edite o `local.properties` na raiz do projeto:

```properties
NGROK_URL=https://sua-url-real.ngrok-free.app/api
```

Essa URL é usada automaticamente quando você seleciona o Build Variant `localDebug`.

> ⚠️ O `local.properties` **não** deve ser commitado no Git (já está no `.gitignore`).

---

## 🔄 Como funciona por dentro

```
Você seleciona "localDebug" no Android Studio
        ↓
Gradle executa task "assembleLocalDebug"
        ↓
detectEnvironmentFromTask() detecta "local"
        ↓
Lê NGROK_URL do local.properties
        ↓
buildkonfig {} gera BuildKonfig.kt com a URL do ngrok
        ↓
ApiConfig.kt lê BuildKonfig.BASE_URL
        ↓
HttpClientFactory.kt usa ApiConfig.baseUrl nas requisições
```

### Constantes geradas (exemplo para `localDebug`):

```kotlin
// Arquivo gerado: composeApp/build/buildkonfig/commonMain/.../BuildKonfig.kt
internal object BuildKonfig {
    val BASE_URL: String = "https://xxxx.ngrok-free.app/api"
    val ENVIRONMENT: String = "local"
    val IS_PRODUCTION: Boolean = false
}
```

---

## ✏️ Onde alterar as URLs

| O que alterar                | Arquivo                          | Linha aprox. |
|------------------------------|----------------------------------|--------------|
| URL de **local** (ngrok)     | `local.properties`               | `NGROK_URL=` |
| URL de **staging**           | `composeApp/build.gradle.kts`    | bloco `when` do `baseUrl` |
| URL de **produção**          | `composeApp/build.gradle.kts`    | bloco `when` do `baseUrl` |
| URL de staging (Android)     | `androidApp/build.gradle.kts`    | flavor `staging` → `buildConfigField` |
| URL de produção (Android)    | `androidApp/build.gradle.kts`    | flavor `production` → `buildConfigField` |

---

## 🧪 Verificar o ambiente no código

```kotlin
// Verificações simples
if (ApiConfig.isLocal) {
    // Lógica de dev local (ex: logs detalhados)
}

if (ApiConfig.isStaging) {
    // Lógica de staging (ex: badge, banner de teste)
}

if (ApiConfig.isProduction) {
    // Lógica de produção
}

// Acessar a URL atual
println("API URL: ${ApiConfig.baseUrl}")
println("Ambiente: ${ApiConfig.currentEnvironment}")

// Override temporário de URL (ex: testes)
ApiConfig.configure("http://10.0.2.2/api")
ApiConfig.reset() // volta ao valor do BuildKonfig
```

---

## 📄 Mapa de Arquivos

```
local.properties                                  → NGROK_URL para dev local
gradle/libs.versions.toml                         → versão + plugin buildkonfig
build.gradle.kts (raiz)                           → buildkonfig apply false
composeApp/build.gradle.kts                       → BuildKonfig config + flavors (library)
androidApp/build.gradle.kts                       → Product Flavors + buildConfigField
composeApp/src/commonMain/.../ApiConfig.kt        → Consome BuildKonfig (compartilhado)
composeApp/src/commonMain/.../HttpClientFactory.kt→ Usa ApiConfig.baseUrl
iosApp/Configuration/Config-Staging.xcconfig      → Config iOS staging
iosApp/Configuration/Config-Production.xcconfig   → Config iOS produção
```

