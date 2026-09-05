# Pixstop Mobile - Kotlin Multiplatform

Aplicativo mobile Pixstop para Android e iOS, construído com Kotlin Multiplatform e Compose Multiplatform.

## 🚀 Funcionalidades

- ✅ Tela de Login com validação
- ✅ Autenticação via API Laravel Sanctum
- ✅ Persistência de token (multiplatform-settings)
- ✅ Cache offline de dados do usuário
- ✅ Navegação automática Login ↔ Home
- ✅ Sidebar com Material Design
- ✅ Ambientes Local, Staging e Produção com URLs separadas
- ✅ Suporte Android e iOS
- ✅ Splash Screen com cores do tema
- ✅ Tema claro/escuro personalizável

## 📁 Estrutura do Projeto

```
├── androidApp/                    # 📱 Módulo Android (aplicação)
│   ├── build.gradle.kts           # Product Flavors (local/staging/production)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/.../MainActivity.kt
│       └── res/
│
├── composeApp/                    # 🔄 Módulo compartilhado (KMP)
│   ├── build.gradle.kts           # BuildKonfig (URLs por ambiente)
│   └── src/
│       ├── commonMain/            # Código compartilhado
│       │   └── kotlin/com/pixstop/mobile/
│       │       ├── core/
│       │       │   ├── config/ApiConfig.kt
│       │       │   ├── network/HttpClientFactory.kt
│       │       │   └── storage/TokenManager.kt
│       │       ├── data/
│       │       ├── ui/
│       │       └── App.kt
│       ├── androidMain/
│       └── iosMain/
│
├── iosApp/                        # 🍎 Módulo iOS (Xcode)
│   └── Configuration/
│       ├── Config.xcconfig
│       ├── Config-Staging.xcconfig
│       └── Config-Production.xcconfig
│
└── docs/                          # 📚 Documentação
    ├── AMBIENTES.md               # Configuração de ambientes deste projeto
    └── GUIA_AMBIENTES_KMP.md      # Guia para replicar em outros projetos
```

## ⚙️ Configuração Rápida

### 1. Configurar URL do Ngrok (dev local)

Edite o `local.properties` na raiz do projeto:

```properties
NGROK_URL=https://sua-url.ngrok-free.app/api
```

### 2. Selecionar o ambiente

No **Android Studio**, selecione o Build Variant no painel lateral:

| Build Variant         | Ambiente   | URL                                    |
|-----------------------|------------|----------------------------------------|
| `localDebug`          | Local      | `NGROK_URL` do `local.properties`      |
| `stagingDebug`        | Staging    | `https://staging.pixstop.com.br/api`   |
| `productionDebug`     | Produção   | `https://pixstop.com.br/api`           |

> O ambiente é detectado **automaticamente** — não precisa passar flags.

### 3. Alterar URLs dos ambientes

Edite o bloco `baseUrl` em `composeApp/build.gradle.kts` (linhas ~155–160).

## 📚 Documentação

| Documento | Descrição |
|-----------|-----------|
| [docs/AMBIENTES.md](docs/AMBIENTES.md) | Configuração completa de ambientes deste projeto |
| [docs/GUIA_AMBIENTES_KMP.md](docs/GUIA_AMBIENTES_KMP.md) | Guia passo a passo para replicar em outros projetos KMP |

## 📦 Dependências Principais

- **Ktor** — Cliente HTTP multiplataforma
- **Kotlinx Serialization** — Serialização JSON
- **Multiplatform Settings** — Persistência de dados
- **Navigation Compose** — Navegação entre telas
- **Compose Multiplatform** — UI compartilhada
- **BuildKonfig** — Constantes de build por ambiente (KMP)

## 🏗️ Build and Run

### Android

```shell
# Local (ngrok)
./gradlew :androidApp:assembleLocalDebug

# Staging
./gradlew :androidApp:assembleStagingDebug

# Produção
./gradlew :androidApp:assembleProductionRelease
```

### iOS

```shell
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -Penvironment=local
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -Penvironment=staging
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -Penvironment=production
```

---

Baseado no [android-login-template](https://github.com/DiegoTracz/android-login-template)
