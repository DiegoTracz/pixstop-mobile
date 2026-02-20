# PixStop Mobile - Kotlin Multiplatform

Aplicativo mobile PixStop para Android e iOS, construído com Kotlin Multiplatform e Compose Multiplatform.

## 🚀 Funcionalidades

- ✅ Tela de Login com validação
- ✅ Autenticação via API Laravel Sanctum
- ✅ Persistência de token (multiplatform-settings)
- ✅ Cache offline de dados do usuário
- ✅ Navegação automática Login ↔ Home
- ✅ Sidebar com Material Design
- ✅ URL da API configurável
- ✅ Suporte Android e iOS
- ✅ Splash Screen com cores do tema
- ✅ Tema claro/escuro personalizável

## 📁 Estrutura do Projeto

```
├── androidApp/                    # 📱 Módulo Android (aplicação)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/.../MainActivity.kt
│       └── res/                   # Recursos Android (ícones, etc)
│
├── composeApp/                    # 🔄 Módulo compartilhado (KMP)
│   └── src/
│       ├── commonMain/            # Código compartilhado
│       │   └── kotlin/com/pixstop/mobile/
│       │       ├── core/
│       │       │   ├── config/ApiConfig.kt      # 🔧 URLs e endpoints
│       │       │   ├── network/HttpClientFactory.kt
│       │       │   └── storage/TokenManager.kt
│       │       ├── data/
│       │       │   ├── model/AuthModels.kt
│       │       │   └── repository/AuthRepository.kt
│       │       ├── ui/
│       │       │   ├── navigation/
│       │       │   ├── screen/
│       │       │   └── viewmodel/
│       │       └── App.kt
│       ├── androidMain/           # Código específico Android
│       └── iosMain/               # Código específico iOS
│
└── iosApp/                        # 🍎 Módulo iOS (Xcode)
```

## ⚙️ Configuração

### 1. Configurar URL da API

Edite o arquivo `core/config/ApiConfig.kt` e altere a constante `BASE_URL`:

```kotlin
private const val BASE_URL = "https://sua-api.com/api"
```

### 2. Configurar Endpoints

Se sua API usar rotas diferentes, edite `ApiConfig.kt`:

```kotlin
object Endpoints {
    const val LOGIN = "/auth/login"
    const val LOGOUT = "/auth/logout"
    const val PROFILE = "/me"
}
```

## 📦 Dependências Principais

- **Ktor** - Cliente HTTP multiplataforma
- **Kotlinx Serialization** - Serialização JSON
- **Multiplatform Settings** - Persistência de dados
- **Navigation Compose** - Navegação entre telas
- **Compose Multiplatform** - UI compartilhada

## 🏗️ Build and Run

### Android

```shell
./gradlew :androidApp:assembleDebug
```

### iOS

Abra o diretório `/iosApp` no Xcode e execute.

---

Baseado no [android-login-template](https://github.com/DiegoTracz/android-login-template)
