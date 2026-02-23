# PixStop Mobile — Componentes UI

> Documentação dos componentes retro 8-bit do PixStop Mobile (Kotlin Multiplatform).
>
> **Tema:** Dark only · Cyberpunk retro 8-bit · Cantos retos (0dp radius) · Fontes pixel
>
> **Data:** Fevereiro 2026

---

## Índice

1. [Design System](#1-design-system)
   - [PixColors](#pixcolors)
   - [PixTypography](#pixtypography)
   - [AppTheme](#apptheme)
2. [Modifiers](#2-modifiers)
   - [pixelBorder](#pixelborder)
   - [pixelShadow](#pixelshadow)
   - [neonGlow](#neonglow)
   - [scanlines](#scanlines)
   - [gridPattern](#gridpattern)
3. [Componentes](#3-componentes)
   - [PixelButton](#pixelbutton)
   - [PixelInput](#pixelinput)
   - [PixelPasswordInput](#pixelpasswordinput)
   - [PixelAuthCard](#pixelauthcard)
   - [PixelAuthHeader](#pixelauthheader)
   - [PixelDivider](#pixeldivider)
   - [PixelLogo](#pixellogo)
   - [AppIcon](#appicon)
   - [QrCodeScannerScreen](#qrcodescannerscreen)
4. [Padrões de Uso](#4-padrões-de-uso)
5. [Guia para Novos Componentes](#5-guia-para-novos-componentes)

---

## 1. Design System

### PixColors

**Arquivo:** `ui/theme/PixColors.kt`

Paleta de cores dark-only do PixStop. Todas as cores são constantes — sem light mode.

| Token | Hex | Uso |
|---|---|---|
| `Dark` | `#050816` | Background principal, fundo de inputs |
| `Darker` | `#030510` | Background secundário |
| `Cyan` | `#00F5D4` | Cor primária, bordas, títulos, links, glow |
| `Green` | `#00E676` | Sucesso, registro, links de ação positiva |
| `Yellow` | `#FFD700` | Alertas, modo offline |
| `Orange` | `#FF9100` | Atenção |
| `Purple` | `#9D4EDD` | Secundário, destaques |
| `Pink` | `#FF6B9D` | Erro, destructive, validação |
| `Blue` | `#00B4D8` | Informativo |
| `Gray100..Gray900` | Escala | Textos, bordas, backgrounds de cards |

**Transparências disponíveis:**

| Token | Uso |
|---|---|
| `CyanAlpha10` / `CyanAlpha20` / `CyanAlpha40` | Backgrounds com glow cyan |
| `GreenAlpha20` | Background sucesso |
| `PinkAlpha20` / `PinkAlpha40` | Background de erro |

```kotlin
// Exemplo de uso
Box(modifier = Modifier.background(PixColors.Dark))
Text("Erro!", color = PixColors.Pink)
Box(modifier = Modifier.background(PixColors.PinkAlpha20))
```

---

### PixTypography

**Arquivo:** `ui/theme/PixTypography.kt`

Duas famílias tipográficas:

| Família | Font | Uso |
|---|---|---|
| **Press Start 2P** | `pixelFontFamily` | Títulos, labels, botões, badges — fonte 8-bit |
| **Inter** | `sansFontFamily` | Texto corrido, descrições, valores de inputs |

**Estilos disponíveis:**

| Estilo | Font | Tamanho | Cor | Uso |
|---|---|---|---|---|
| `pageTitle` | Press Start 2P | 16sp | Cyan | Título principal de página |
| `authTitle` | Press Start 2P | 14sp | Cyan | Título de auth cards |
| `sectionTitle` | Press Start 2P | 12sp | Cyan | Títulos de seção |
| `branding` | Press Start 2P | 11sp | Cyan | Nome "PixStop" |
| `buttonText` | Press Start 2P | 10sp | — | Texto de botões |
| `buttonTextSm` | Press Start 2P | 8sp | — | Texto de botões small |
| `inputLabel` | Press Start 2P | 8sp | Cyan | Labels de inputs |
| `terminalHeader` | Press Start 2P | 7sp | Cyan 60% | Header do terminal card |
| `badgeText` | Press Start 2P | 6sp | — | Badges e dividers |
| `errorText` | Press Start 2P | 6sp | Pink | Mensagens de erro |
| `footerText` | Press Start 2P | 6sp | Cyan 40% | Rodapés |
| `bodyRegular` | Inter | 14sp | Gray100 | Texto corrido |
| `bodySecondary` | Inter | 12sp | Gray400 | Texto secundário |
| `bodyMuted` | Inter | 12sp | Gray300 | Texto discreto |
| `placeholder` | Inter | 14sp | Gray500 | Placeholder de inputs |
| `link` | Inter Medium | 14sp | Cyan | Links clicáveis |
| `inputValue` | Inter | 14sp | Gray100 | Valor digitado nos inputs |

```kotlin
// Exemplo de uso
Text("TÍTULO", style = PixTypography.pageTitle)
Text("Descrição", style = PixTypography.bodySecondary)
Text("LABEL", style = PixTypography.inputLabel)
```

> **⚠️ Nota:** Todos os estilos que usam fontes são `@Composable get()` — só podem ser acessados dentro de contexto `@Composable`.

---

### AppTheme

**Arquivo:** `ui/theme/AppTheme.kt`

Tema Material3 dark-only. Mapeamento de `PixColors` → `darkColorScheme()`.

```kotlin
@Composable
fun AppTheme(content: @Composable () -> Unit)
```

Não aceita parâmetro `darkTheme` — é sempre dark.

---

## 2. Modifiers

**Arquivo:** `ui/components/PixelModifiers.kt`

Modifier extensions para efeitos retro 8-bit reutilizáveis.

### pixelBorder

Borda sólida pixel com cantos retos (0dp radius).

```kotlin
fun Modifier.pixelBorder(
    color: Color = PixColors.Cyan,
    width: Dp = 2.dp
): Modifier
```

```kotlin
// Exemplo
Box(modifier = Modifier
    .size(100.dp)
    .background(PixColors.Gray800)
    .pixelBorder(PixColors.Pink, 3.dp)
)
```

---

### pixelShadow

Sombra sólida sem blur — estilo retro 8-bit. Desenhada atrás do componente.

```kotlin
fun Modifier.pixelShadow(
    color: Color = PixColors.Cyan,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp
): Modifier
```

```kotlin
// Exemplo
Box(modifier = Modifier
    .pixelShadow(PixColors.Cyan, 4.dp, 4.dp)
    .background(PixColors.Gray800)
    .size(80.dp)
)
```

---

### neonGlow

Glow neon ao redor do componente — retângulo expandido com alpha.

```kotlin
fun Modifier.neonGlow(
    color: Color = PixColors.Cyan,
    radius: Dp = 20.dp,
    alpha: Float = 0.4f
): Modifier
```

```kotlin
// Exemplo — glow no foco de input
Modifier.neonGlow(color = PixColors.Cyan, radius = 8.dp, alpha = 0.15f)
```

---

### scanlines

Overlay CRT — linhas horizontais semitransparentes desenhadas sobre o conteúdo.

```kotlin
fun Modifier.scanlines(): Modifier
```

```kotlin
// Exemplo — usar no background principal
Box(modifier = Modifier.fillMaxSize().background(PixColors.Dark).scanlines())
```

---

### gridPattern

Grid decorativo — linhas ciano finas simulando tela digital.

```kotlin
fun Modifier.gridPattern(
    color: Color = PixColors.Cyan.copy(alpha = 0.03f),
    spacing: Dp = 40.dp
): Modifier
```

```kotlin
// Exemplo — combinar com scanlines para efeito completo
Box(modifier = Modifier
    .fillMaxSize()
    .background(PixColors.Dark)
    .gridPattern()
    .scanlines()
)
```

---

## 3. Componentes

### PixelButton

**Arquivo:** `ui/components/PixelButton.kt`

Botão retro 8-bit com sombra pixel sólida, cantos retos, animação de press e estados disabled/loading.

```kotlin
@Composable
fun PixelButton(
    text: String,                                          // Texto (será uppercase)
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PixelButtonVariant = Primary,                 // Primary | Secondary | Destructive
    buttonSize: PixelButtonSize = Default,                 // Small | Default | Large
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String? = null,                           // Texto durante loading
    icon: (@Composable () -> Unit)? = null                 // Ícone à esquerda
)
```

**Variantes:**

| Variante | Background | Texto | Borda |
|---|---|---|---|
| `Primary` | Cyan | Dark | Cyan |
| `Secondary` | Transparent | Cyan | Cyan |
| `Destructive` | Pink | White | Pink |

**Tamanhos:**

| Tamanho | Padding H | Padding V | Font |
|---|---|---|---|
| `Small` | 16dp | 8dp | 8sp |
| `Default` | 20dp | 10dp | 10sp |
| `Large` | 24dp | 12dp | 10sp |

**Comportamento:**
- Sombra 4dp no estado normal, 2dp quando pressionado, 0dp quando disabled/loading
- O botão se desloca 2dp ao ser pressionado (efeito "push")
- Opacity 50% quando disabled ou loading
- Texto sempre em UPPERCASE

```kotlin
// Exemplos
PixelButton(
    text = "ENTRAR",
    onClick = { /* ... */ },
    modifier = Modifier.fillMaxWidth(),
    isLoading = isLoading,
    loadingText = "ENTRANDO..."
)

PixelButton(
    text = "Cancelar",
    onClick = { /* ... */ },
    variant = PixelButtonVariant.Secondary
)

PixelButton(
    text = "Excluir",
    onClick = { /* ... */ },
    variant = PixelButtonVariant.Destructive
)
```

---

### PixelInput

**Arquivo:** `ui/components/PixelInput.kt`

Input de texto retro 8-bit com label pixel, borda sólida, glow no foco, erro em pink.

```kotlin
@Composable
fun PixelInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,                                          // Será uppercase automaticamente
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,                                  // Exibe msg em pink abaixo do input
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = None,
    keyboardOptions: KeyboardOptions = Default,
    keyboardActions: KeyboardActions = Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null
)
```

**Estados visuais:**

| Estado | Borda | Glow |
|---|---|---|
| Normal | Gray600 | Nenhum |
| Foco | Cyan | Cyan 15% |
| Erro | Pink | Pink 15% |

```kotlin
// Exemplo com ícone e erro
PixelInput(
    value = email,
    onValueChange = { email = it },
    label = "E-MAIL",
    placeholder = "seu@email.com",
    error = if (emailInvalido) "E-mail inválido" else null,
    leadingIcon = {
        AppIcon(
            icon = AppIconType.Email,
            contentDescription = null,
            tint = PixColors.Gray400,
            modifier = Modifier.size(16.dp)
        )
    }
)
```

---

### PixelPasswordInput

**Arquivo:** `ui/components/PixelPasswordInput.kt`

Wrapper do `PixelInput` especializado para senhas com toggle de visibilidade.

```kotlin
@Composable
fun PixelPasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "••••••••",
    error: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(Password, Done),
    keyboardActions: KeyboardActions = Default
)
```

- Ícone olho à direita para toggle visibilidade
- `PasswordVisualTransformation` quando oculto
- Herda todos os estados visuais do `PixelInput`

```kotlin
// Exemplo
PixelPasswordInput(
    value = password,
    onValueChange = { password = it },
    label = "SENHA",
    error = passwordError
)
```

---

### PixelAuthCard

**Arquivo:** `ui/components/PixelAuthCard.kt`

Container terminal fullscreen para telas de autenticação. Ocupa toda a largura no mobile.

```kotlin
@Composable
fun PixelAuthCard(
    modifier: Modifier = Modifier,
    headerTitle: String = "SYSTEM.AUTH",
    content: @Composable ColumnScope.() -> Unit
)
```

**Estrutura:**
```
┌─────────────────────────────────────┐
│ ● ● ●  SYSTEM.AUTH                  │  ← Terminal header (Gray800)
├─────────────────────────────────────┤     3 dots: Pink, Yellow, Green
│                                     │
│         (conteúdo)                  │  ← Padding 20dp H, 24dp V
│                                     │     Background: Gray900
└─────────────────────────────────────┘
```

```kotlin
// Exemplo
PixelAuthCard(headerTitle = "SYSTEM.REGISTER") {
    PixelAuthHeader(
        icon = AppIconType.PersonAdd,
        title = "CRIAR CONTA",
        subtitle = "Registre-se"
    )
    Spacer(modifier = Modifier.height(32.dp))
    PixelInput(/* ... */)
    Spacer(modifier = Modifier.height(20.dp))
    PixelButton(text = "REGISTRAR", onClick = { })
}
```

---

### PixelAuthHeader

**Arquivo:** `ui/components/PixelAuthHeader.kt`

Header para telas de autenticação com ícone colorido, glow, título e subtítulo.

```kotlin
@Composable
fun PixelAuthHeader(
    icon: AppIconType,
    title: String,                      // Será uppercase
    subtitle: String,
    modifier: Modifier = Modifier,
    themeColor: Color = PixColors.Cyan
)
```

**Estrutura:**
```
        ┌──────────┐
        │   ICON   │  ← 64x64, bg = themeColor, glow neon
        └──────────┘
         TÍTULO        ← Press Start 2P 14sp, themeColor
        Subtítulo      ← Inter 12sp, Gray400
```

```kotlin
// Exemplo — Login (cyan)
PixelAuthHeader(
    icon = AppIconType.Lock,
    title = "LOGIN",
    subtitle = "Acesse sua conta",
    themeColor = PixColors.Cyan
)

// Exemplo — Registro (green)
PixelAuthHeader(
    icon = AppIconType.PersonAdd,
    title = "CRIAR CONTA",
    subtitle = "Registre-se para continuar",
    themeColor = PixColors.Green
)
```

---

### PixelDivider

**Arquivo:** `ui/components/PixelDivider.kt`

Divider retro com texto centralizado.

```kotlin
@Composable
fun PixelDivider(
    modifier: Modifier = Modifier,
    text: String = "OU"
)
```

**Visual:**
```
────────────── OU ──────────────
```
- Linha: 2dp, Gray700
- Texto: badgeText (6sp Press Start 2P), Gray500
- Padding vertical: 16dp

```kotlin
// Exemplo
PixelDivider()                    // "OU"
PixelDivider(text = "OU ENTÃO")   // Custom
```

---

### PixelLogo

**Arquivo:** `ui/components/PixelLogo.kt`

Logo pixel art "P" do PixStop desenhado em Canvas. Grid 24×24 com blocos 4×4.

```kotlin
@Composable
fun PixelLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = PixColors.Cyan,
    accentAlpha: Float = 0.6f
)
```

```kotlin
// Exemplo
PixelLogo(size = 120.dp)
PixelLogo(size = 48.dp, color = PixColors.Green)
```

---

### AppIcon

**Arquivo:** `ui/components/AppIcon.kt` (expect) + `AppIcon.android.kt` + `AppIcon.ios.kt`

Componente de ícone multiplataforma.

```kotlin
@Composable
expect fun AppIcon(
    icon: AppIconType,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null
)
```

- **Android:** Material Icons (Filled + AutoMirrored)
- **iOS:** SF Symbols

**Ícones disponíveis (`AppIconType`):**

| Categoria | Ícones |
|---|---|
| Navigation | `Menu`, `Home`, `Search`, `Notifications`, `Settings` |
| User | `Person`, `PersonAdd`, `Logout` |
| Auth | `Lock`, `Email`, `Visibility`, `VisibilityOff` |
| Actions | `Refresh`, `Add`, `Edit`, `Delete`, `Share`, `QrCodeScanner` |
| Status | `Check`, `Close`, `Info`, `Warning` |
| Navigation Extras | `ChevronDown`, `ChevronUp`, `ArrowBack` |

**Para adicionar novos ícones:**

1. Adicionar entrada no `enum AppIconType` em `AppIcon.kt`
2. Mapear para Material Icon em `AppIcon.android.kt`
3. Mapear para SF Symbol em `AppIcon.ios.kt`

```kotlin
// Exemplo
AppIcon(
    icon = AppIconType.Email,
    contentDescription = "Email",
    tint = PixColors.Cyan,
    modifier = Modifier.size(20.dp)
)
```

---

### QrCodeScannerScreen

**Arquivo:** `ui/components/QrCodeScanner.kt` (expect) + `.android.kt` + `.ios.kt`

Scanner de QR Code fullscreen multiplataforma.

```kotlin
@Composable
expect fun QrCodeScannerScreen(
    onCodeScanned: (String) -> Unit,    // Callback com valor bruto do QR
    onDismiss: () -> Unit               // Fechar scanner
)
```

- **Android:** CameraX + ML Kit Barcode Scanning
- **iOS:** AVFoundation + AVCaptureMetadataOutput

**Dependências Android (em `composeApp/build.gradle.kts`):**
```kotlin
implementation("androidx.camera:camera-camera2:1.4.2")
implementation("androidx.camera:camera-lifecycle:1.4.2")
implementation("androidx.camera:camera-view:1.4.2")
implementation("com.google.mlkit:barcode-scanning:17.3.0")
```

**Permissões:**
- Android: `CAMERA` em `AndroidManifest.xml` (request em runtime)
- iOS: `NSCameraUsageDescription` em `Info.plist`

**Visual:** Fundo escuro com preview da câmera, frame de scanning com cantos cyan, top bar com botão fechar e bottom bar com instrução.

```kotlin
// Exemplo — usar com CompanyCodeParser
QrCodeScannerScreen(
    onCodeScanned = { rawValue ->
        val code = CompanyCodeParser.parse(rawValue)
        // code = "CONFIALMQYNV" extraído de
        // "https://pixstop.com.br/register/user?code=CONFIALMQYNV"
    },
    onDismiss = { /* fechar */ }
)
```

---

## 4. Padrões de Uso

### Background padrão para telas

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(PixColors.Dark)
        .gridPattern()
        .scanlines()
)
```

### Box de erro

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(PixColors.PinkAlpha20)
        .pixelBorder(PixColors.Pink)
        .padding(12.dp)
) {
    Text(text = errorMessage, style = PixTypography.errorText)
}
```

### Tela de autenticação completa

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(PixColors.Dark)
        .gridPattern()
        .scanlines()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PixStop", style = PixTypography.pageTitle)
        Spacer(modifier = Modifier.height(32.dp))

        PixelAuthCard {
            PixelAuthHeader(
                icon = AppIconType.Lock,
                title = "LOGIN",
                subtitle = "Acesse sua conta"
            )
            Spacer(modifier = Modifier.height(32.dp))
            PixelInput(/* ... */)
            Spacer(modifier = Modifier.height(20.dp))
            PixelPasswordInput(/* ... */)
            Spacer(modifier = Modifier.height(24.dp))
            PixelButton(text = "ENTRAR", onClick = { })
            PixelDivider()
            // Link para outra tela
            Row { /* ... */ }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("© 2026 PixStop", style = PixTypography.footerText)
    }
}
```

### Espaçamentos recomendados

| Entre | Espaçamento |
|---|---|
| Título → Card | 32dp |
| Header → Inputs | 32dp |
| Input → Input | 20dp |
| Último input → Botão | 24dp |
| Botão → Divider | automático (16dp via PixelDivider) |
| Card → Footer | 32dp |

---

## 5. Guia para Novos Componentes

### Princípios obrigatórios

1. **Cantos retos** — nunca usar `RoundedCornerShape` ou `CircleShape`
2. **Dark only** — usar `PixColors`, nunca `MaterialTheme.colorScheme` diretamente
3. **Fontes pixel** — títulos/labels/botões em `Press Start 2P`, corpo em `Inter`
4. **Bordas sólidas** — usar `pixelBorder()` ao invés de `border()` com shape
5. **Sem ripple** — usar `indication = null` no `clickable`
6. **Texto uppercase** — labels, botões e títulos sempre em UPPERCASE

### Template para novo componente

```kotlin
package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * [Descrição do componente]
 */
@Composable
fun PixelNomeDoComponente(
    // params obrigatórios primeiro
    // modifier sempre com default
    modifier: Modifier = Modifier,
    // params opcionais
) {
    // Usar PixColors para cores
    // Usar PixTypography para textos
    // Usar pixelBorder() para bordas
    // Cantos retos sempre (0dp)
    Box(
        modifier = modifier
            .background(PixColors.Gray900)
            .pixelBorder(PixColors.Cyan)
    ) {
        // ...
    }
}
```

### Checklist para PR

- [ ] Usa `PixColors` (não cores hardcoded)
- [ ] Usa `PixTypography` (não `MaterialTheme.typography`)
- [ ] Cantos retos (0dp radius)
- [ ] Sem ripple effect
- [ ] Labels/títulos em UPPERCASE
- [ ] Funciona em Android e iOS
- [ ] Documentado neste arquivo

---

## Estrutura de arquivos

```
composeApp/src/
├── commonMain/kotlin/com/pixstop/mobile/
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── PixColors.kt         ← Paleta de cores
│   │   │   ├── PixTypography.kt     ← Tipografia (Press Start 2P + Inter)
│   │   │   └── AppTheme.kt          ← MaterialTheme dark-only
│   │   ├── components/
│   │   │   ├── PixelButton.kt       ← Botão retro 8-bit
│   │   │   ├── PixelInput.kt        ← Input com label pixel
│   │   │   ├── PixelPasswordInput.kt← Input de senha
│   │   │   ├── PixelAuthCard.kt     ← Card terminal fullscreen
│   │   │   ├── PixelAuthHeader.kt   ← Header com ícone e título
│   │   │   ├── PixelDivider.kt      ← Divider "── OU ──"
│   │   │   ├── PixelLogo.kt         ← Logo pixel art "P"
│   │   │   ├── PixelModifiers.kt    ← Modifiers reutilizáveis
│   │   │   ├── AppIcon.kt           ← expect AppIcon + AppIconType
│   │   │   └── QrCodeScanner.kt     ← expect QrCodeScannerScreen
│   │   ├── screen/
│   │   │   ├── SplashScreen.kt
│   │   │   ├── LoginScreen.kt
│   │   │   ├── RegisterScreen.kt
│   │   │   └── HomeScreen.kt
│   │   └── viewmodel/
│   │       ├── LoginViewModel.kt
│   │       └── RegisterViewModel.kt
│   └── core/
│       └── config/
│           └── CompanyCodeParser.kt  ← Extrai code de URL do QR
│
├── androidMain/kotlin/.../ui/components/
│   ├── AppIcon.android.kt           ← Material Icons
│   └── QrCodeScanner.android.kt     ← CameraX + ML Kit
│
├── iosMain/kotlin/.../ui/components/
│   ├── AppIcon.ios.kt               ← SF Symbols
│   └── QrCodeScanner.ios.kt         ← AVFoundation
│
└── commonMain/composeResources/
    ├── font/
    │   ├── press_start_2p.ttf
    │   └── inter_regular.ttf
    └── drawable/
        └── compose-multiplatform.xml
```

