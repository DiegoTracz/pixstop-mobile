# Guia de Estilo Mobile — PixStop 8-Bit Retro

Guia de design para o aplicativo mobile nativo (Android/Kotlin + iOS/Swift) do PixStop, extraído diretamente do design system web retro 8-bit.

> **Fonte:** [`resources/css/app.css`](../resources/css/app.css) · [`PIXEL-COMPONENTS.md`](../resources/js/components/pixel/PIXEL-COMPONENTS.md)

---

## Índice

1. [Identidade Visual](#1-identidade-visual)
2. [Paleta de Cores](#2-paleta-de-cores)
3. [Tipografia](#3-tipografia)
4. [Sombras Pixel](#4-sombras-pixel)
5. [Animações](#5-animações)
6. [Efeitos Visuais Retro](#6-efeitos-visuais-retro)
7. [Componentes](#7-componentes)
8. [Ícones](#8-ícones)
9. [Espaçamentos](#9-espaçamentos)
10. [Navegação](#10-navegação)
11. [Referências](#11-referências)

---

## 1. Identidade Visual

**Nome:** PixStop
**Tema:** Dark cyberpunk retro 8-bit com acentos neon
**Modo:** Dark only (sem light mode no app mobile)

### Logo — Pixel Art "P"

O logo é uma letra "P" construída a partir de retângulos (pixels) em SVG. Reproduzir nativamente usando um grid de quadrados.

**Grid 24×24 (coordenadas dos blocos 4×4):**

```
      x4   x8   x12  x16
y2    [██] [██] [██]
y6    [██]      [██]
y10   [██] [██] [██]
y14   [██]
y18   [██]           [··]   ← ponto accent (60% opacidade)
```

**Coordenadas precisas (x, y, largura, altura):**

| Bloco | x  | y  | w | h | Opacidade |
|-------|----|----|---|---|-----------|
| 1     | 4  | 2  | 4 | 4 | 100%      |
| 2     | 8  | 2  | 4 | 4 | 100%      |
| 3     | 12 | 2  | 4 | 4 | 100%      |
| 4     | 4  | 6  | 4 | 4 | 100%      |
| 5     | 12 | 6  | 4 | 4 | 100%      |
| 6     | 4  | 10 | 4 | 4 | 100%      |
| 7     | 8  | 10 | 4 | 4 | 100%      |
| 8     | 12 | 10 | 4 | 4 | 100%      |
| 9     | 4  | 14 | 4 | 4 | 100%      |
| 10    | 4  | 18 | 4 | 4 | 100%      |
| Accent| 16 | 18 | 4 | 4 | 60%       |

**Implementação:**

- **Android (Compose):** `Canvas` com `drawRect` para cada bloco
- **iOS (SwiftUI):** `Path` com `addRect` ou `Canvas` com `fill(CGRect(...))`
- Cor do logo: `pix-cyan` (`#00F5D4`) sobre fundo `pix-dark` (`#050816`)

---

## 2. Paleta de Cores

### 2.1 Cores Primárias

| Token          | Hex       | ARGB (Android)   | Uso                          |
|----------------|-----------|-------------------|------------------------------|
| `pix-dark`     | `#050816` | `0xFF050816`      | Background principal         |
| `pix-darker`   | `#030510` | `0xFF030510`      | Background secundário/navbar |
| `pix-cyan`     | `#00F5D4` | `0xFF00F5D4`      | **Cor primária** — botões, bordas, foco, títulos |

### 2.2 Cores Semânticas

| Token          | Hex       | ARGB (Android)   | Uso                       |
|----------------|-----------|-------------------|---------------------------|
| `pix-green`    | `#00E676` | `0xFF00E676`      | Sucesso, confirmações     |
| `pix-yellow`   | `#FFD700` | `0xFFFFD700`      | Alertas, destaques        |
| `pix-orange`   | `#FF9100` | `0xFFFF9100`      | Ações secundárias         |
| `pix-purple`   | `#9D4EDD` | `0xFF9D4EDD`      | Accent alternativo        |
| `pix-pink`     | `#FF6B9D` | `0xFFFF6B9D`      | Erro, ações destrutivas   |
| `pix-blue`     | `#00B4D8` | `0xFF00B4D8`      | Info, links               |

### 2.3 Escala de Cinza

| Token            | Hex       | ARGB (Android)   | Uso                           |
|------------------|-----------|-------------------|-------------------------------|
| `pix-gray-100`   | `#E2E8F0` | `0xFFE2E8F0`      | Texto principal               |
| `pix-gray-200`   | `#CBD5E1` | `0xFFCBD5E1`      | Texto secundário              |
| `pix-gray-300`   | `#94A3B8` | `0xFF94A3B8`      | Texto muted                   |
| `pix-gray-400`   | `#64748B` | `0xFF64748B`      | Descrições, placeholders      |
| `pix-gray-500`   | `#475569` | `0xFF475569`      | Placeholders de input         |
| `pix-gray-600`   | `#334155` | `0xFF334155`      | Bordas de input, divisores    |
| `pix-gray-700`   | `#1E293B` | `0xFF1E293B`      | Bordas, backgrounds secundários |
| `pix-gray-800`   | `#0F172A` | `0xFF0F172A`      | Background de cards           |
| `pix-gray-900`   | `#0A0F1A` | `0xFF0A0F1A`      | Background profundo de cards  |

### 2.4 Mapeamento Semântico

| Papel               | Cor             | Hex       |
|----------------------|-----------------|-----------|
| Background           | `pix-dark`      | `#050816` |
| Foreground (texto)   | `pix-gray-100`  | `#E2E8F0` |
| Card background      | `pix-gray-800`  | `#0F172A` |
| Primary              | `pix-cyan`      | `#00F5D4` |
| Primary foreground   | `pix-dark`      | `#050816` |
| Accent               | `pix-purple`    | `#9D4EDD` |
| Destructive          | `pix-pink`      | `#FF6B9D` |
| Border               | `pix-gray-600`  | `#334155` |
| Ring / Focus         | `pix-cyan`      | `#00F5D4` |
| Sidebar background   | `pix-darker`    | `#030510` |
| Muted text           | `pix-gray-300`  | `#94A3B8` |
| Input background     | `pix-gray-700`  | `#1E293B` |

### 2.5 Cores de Gráficos

| Token    | Hex       | Uso                  |
|----------|-----------|----------------------|
| chart-1  | `#00F5D4` | Gráfico primário     |
| chart-2  | `#00E676` | Gráfico secundário   |
| chart-3  | `#FFD700` | Gráfico terciário    |
| chart-4  | `#9D4EDD` | Gráfico quaternário  |
| chart-5  | `#FF6B9D` | Gráfico quinário     |

### 2.6 Implementação por Plataforma

**Android (Kotlin — colors.xml ou Compose):**

```kotlin
// Compose — Color.kt
object PixColors {
    val Dark       = Color(0xFF050816)
    val Darker     = Color(0xFF030510)
    val Cyan       = Color(0xFF00F5D4)
    val Green      = Color(0xFF00E676)
    val Yellow     = Color(0xFFFFD700)
    val Orange     = Color(0xFFFF9100)
    val Purple     = Color(0xFF9D4EDD)
    val Pink       = Color(0xFFFF6B9D)
    val Blue       = Color(0xFF00B4D8)

    val Gray100    = Color(0xFFE2E8F0)
    val Gray200    = Color(0xFFCBD5E1)
    val Gray300    = Color(0xFF94A3B8)
    val Gray400    = Color(0xFF64748B)
    val Gray500    = Color(0xFF475569)
    val Gray600    = Color(0xFF334155)
    val Gray700    = Color(0xFF1E293B)
    val Gray800    = Color(0xFF0F172A)
    val Gray900    = Color(0xFF0A0F1A)
}
```

**iOS (Swift — Color extension):**

```swift
extension Color {
    static let pixDark      = Color(hex: 0x050816)
    static let pixDarker    = Color(hex: 0x030510)
    static let pixCyan      = Color(hex: 0x00F5D4)
    static let pixGreen     = Color(hex: 0x00E676)
    static let pixYellow    = Color(hex: 0xFFD700)
    static let pixOrange    = Color(hex: 0xFF9100)
    static let pixPurple    = Color(hex: 0x9D4EDD)
    static let pixPink      = Color(hex: 0xFF6B9D)
    static let pixBlue      = Color(hex: 0x00B4D8)

    static let pixGray100   = Color(hex: 0xE2E8F0)
    static let pixGray200   = Color(hex: 0xCBD5E1)
    static let pixGray300   = Color(hex: 0x94A3B8)
    static let pixGray400   = Color(hex: 0x64748B)
    static let pixGray500   = Color(hex: 0x475569)
    static let pixGray600   = Color(hex: 0x334155)
    static let pixGray700   = Color(hex: 0x1E293B)
    static let pixGray800   = Color(hex: 0x0F172A)
    static let pixGray900   = Color(hex: 0x0A0F1A)

    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }
}
```

---

## 3. Tipografia

### 3.1 Famílias Tipográficas

| Token          | Fonte               | Origem        | Uso                               |
|----------------|----------------------|---------------|------------------------------------|
| `font-pixel`   | **Press Start 2P**   | Google Fonts  | Títulos, labels, botões, badges — fonte 8-bit |
| `font-sans`    | **Inter** (400–800)  | Google Fonts  | Texto corrido, descrições, valores de formulários |

**Download:**

- [Press Start 2P](https://fonts.google.com/specimen/Press+Start+2P) — incluir no bundle do app
- [Inter](https://fonts.google.com/specimen/Inter) — pesos: 400 (Regular), 500 (Medium), 600 (SemiBold), 700 (Bold), 800 (ExtraBold)

### 3.2 Escala Tipográfica

| Contexto                 | Fonte          | Tamanho (sp/pt) | Peso     | Estilo              | Cor             |
|--------------------------|----------------|-----------------|----------|----------------------|-----------------|
| Título de página         | Press Start 2P | 14–16           | Regular  | Normal               | `pix-cyan`      |
| Título de seção          | Press Start 2P | 12              | Regular  | Normal               | `pix-cyan`      |
| Label de input           | Press Start 2P | 8               | Regular  | Normal               | `pix-cyan`      |
| Texto de botão           | Press Start 2P | 10              | Regular  | Uppercase, tracking +2 | Varia por variante |
| Badge                    | Press Start 2P | 6               | Regular  | Uppercase            | Varia por cor   |
| Texto de erro            | Press Start 2P | 6               | Regular  | Normal               | `pix-pink`      |
| Branding "PixStop"       | Press Start 2P | 11              | Regular  | Normal               | `pix-cyan`      |
| Footer / copyright       | Press Start 2P | 6               | Regular  | Normal               | `pix-cyan` 40%  |
| Texto corpo              | Inter          | 14              | Regular  | Normal               | `pix-gray-100`  |
| Texto secundário         | Inter          | 12              | Regular  | Normal               | `pix-gray-400`  |
| Texto muted              | Inter          | 12              | Regular  | Normal               | `pix-gray-300`  |
| Placeholder              | Inter          | 14              | Regular  | Normal               | `pix-gray-500`  |
| Link                     | Inter          | 14              | Medium   | Underline            | `pix-cyan`      |

### 3.3 Notas de Implementação

**Press Start 2P** é uma fonte bitmap projetada para renderização em tamanhos pequenos. Considerar:

- Nunca aplicar anti-aliasing suavizado — manter a renderização pixelada quando possível
- Android: `android:typeface` com o `.ttf` em `res/font/` ou Compose `FontFamily(Font(R.font.press_start_2p))`
- iOS: Registrar via `Info.plist` → `Fonts provided by application`, usar `Font.custom("PressStart2P-Regular", size:)`
- **Tracking/Letter-spacing** em botões: +2sp (Android `.sp`), +2pt (iOS)
- **Uppercase transforms:** Sempre via código (`uppercased()` / `.uppercase()`), nunca confiar na fonte

---

## 4. Sombras Pixel

O estilo retro 8-bit é definido por **sombras sólidas sem blur** (offset puro), simulando profundidade de sprites antigos.

### 4.1 Sombras Sólidas (Pixel Shadows)

| Token             | Offset X | Offset Y | Blur | Cor                                          |
|-------------------|----------|----------|------|----------------------------------------------|
| `pixel-sm`        | 2dp      | 2dp      | 0    | `currentColor` (herda do componente)         |
| `pixel`           | 4dp      | 4dp      | 0    | `currentColor`                               |
| `pixel-lg`        | 6dp      | 6dp      | 0    | `currentColor`                               |
| `pixel-cyan`      | 4dp      | 4dp      | 0    | `#00F5D4`                                    |
| `pixel-green`     | 4dp      | 4dp      | 0    | `#00E676`                                    |
| `pixel-yellow`    | 4dp      | 4dp      | 0    | `#FFD700`                                    |

### 4.2 Sombras Neon Glow

Usadas em estados de foco/ativo e elementos decorativos.

| Token             | Offset | Blur  | Cor                           |
|-------------------|--------|-------|-------------------------------|
| `glow-cyan`       | 0, 0   | 20dp  | `rgba(0, 245, 212, 0.4)`     |
| `glow-green`      | 0, 0   | 20dp  | `rgba(0, 230, 118, 0.4)`     |
| `glow-yellow`     | 0, 0   | 20dp  | `rgba(255, 215, 0, 0.4)`     |
| `glow-pink`       | 0, 0   | 20dp  | `rgba(255, 107, 157, 0.4)`   |
| `glow-red`        | 0, 0   | 20dp  | `rgba(239, 68, 68, 0.5)`     |

### 4.3 Implementação por Plataforma

**Android:**

A `elevation` padrão do Android NÃO serve para sombras pixel — ela gera blur e offset baseado em iluminação material. Usar abordagens alternativas:

```kotlin
// Compose — Sombra pixel via Modifier.drawBehind
fun Modifier.pixelShadow(
    color: Color = PixColors.Cyan,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp
) = this.drawBehind {
    drawRect(
        color = color,
        topLeft = Offset(offsetX.toPx(), offsetY.toPx()),
        size = size
    )
}

// Glow neon via Modifier.drawBehind
fun Modifier.neonGlow(
    color: Color = PixColors.Cyan,
    radius: Dp = 20.dp,
    alpha: Float = 0.4f
) = this.drawBehind {
    drawRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(-radius.toPx(), -radius.toPx()),
        size = Size(size.width + radius.toPx() * 2, size.height + radius.toPx() * 2),
        style = Fill
    )
    // Alternativa: usar Paint com BlurMaskFilter
}
```

**iOS:**

```swift
// SwiftUI — Sombra pixel via overlay
extension View {
    func pixelShadow(color: Color = .pixCyan, x: CGFloat = 4, y: CGFloat = 4) -> some View {
        self.background(
            Rectangle()
                .fill(color)
                .offset(x: x, y: y)
        )
    }

    func neonGlow(color: Color = .pixCyan, radius: CGFloat = 20, opacity: Double = 0.4) -> some View {
        self.shadow(color: color.opacity(opacity), radius: radius, x: 0, y: 0)
    }
}

// UIKit — layer shadows (sem blur para pixel shadow)
view.layer.shadowColor = UIColor(hex: 0x00F5D4).cgColor
view.layer.shadowOffset = CGSize(width: 4, height: 4)
view.layer.shadowRadius = 0  // ZERO blur = sombra pixel sólida
view.layer.shadowOpacity = 1.0
```

---

## 5. Animações

Todas as animações do PixStop seguem o estilo retro com transições abruptas (`step-end`) ou fluidas conforme o contexto.

### 5.1 Catálogo de Animações

| Nome           | Duração | Easing          | Loop      | Descrição                                    |
|----------------|---------|-----------------|-----------|----------------------------------------------|
| `pixelBlink`   | 1000ms  | **Step-end**    | ∞ Infinito| Cursor piscando: opacidade 0↔1 sem transição suave |
| `float`        | 3000ms  | Ease-in-out     | ∞ Infinito| Flutuação suave no eixo Y (-10dp)            |
| `glitch`       | 300ms   | Ease-in-out     | Uma vez   | Translação aleatória X/Y ±2dp (efeito digital)|
| `coinSpin`     | 600ms   | Ease-in-out     | Uma vez   | Rotação 360° no eixo Y (flip de moeda)       |
| `slideUp`      | 500ms   | Ease-out        | Uma vez   | Fade in + translação Y de 20dp→0             |
| `pulseGlow`    | 2000ms  | Ease-in-out     | ∞ Infinito| Pulsar raio de glow 20dp→40dp                |
| `floatUp`      | Variável| Linear          | ∞ Infinito| Partículas flutuando de baixo para cima com rotação |

### 5.2 Keyframes Detalhados

#### pixelBlink (cursor terminal)

```
0%   → opacity: 1.0
50%  → opacity: 0.0
100% → opacity: 1.0
Interpolação: STEP (sem suavização — abrupto)
```

#### float (flutuação)

```
0%   → translateY: 0dp
50%  → translateY: -10dp
100% → translateY: 0dp
Interpolação: ease-in-out
```

#### glitch (efeito digital)

```
0%   → translate(0, 0)
20%  → translate(-2dp, 2dp)
40%  → translate(-2dp, -2dp)
60%  → translate(2dp, 2dp)
80%  → translate(2dp, -2dp)
100% → translate(0, 0)
Interpolação: ease-in-out
```

#### coinSpin (moeda)

```
0%   → rotateY: 0°
100% → rotateY: 360°
Interpolação: ease-in-out
```

#### slideUp (entrada)

```
0%   → opacity: 0.0, translateY: 20dp
100% → opacity: 1.0, translateY: 0dp
Interpolação: ease-out (desacelera no final)
```

#### pulseGlow (pulso neon)

```
0%   → shadow blur: 20dp, alpha: 0.4
50%  → shadow blur: 40dp, alpha: 0.8
100% → shadow blur: 20dp, alpha: 0.4
Interpolação: ease-in-out
```

### 5.3 Implementação por Plataforma

**Android (Compose):**

```kotlin
// pixelBlink
val blinkAlpha by rememberInfiniteTransition().animateFloat(
    initialValue = 1f, targetValue = 0f,
    animationSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = 1000
            1f at 0 using LinearEasing
            1f at 499 using LinearEasing
            0f at 500 using LinearEasing
            0f at 999 using LinearEasing
        },
        repeatMode = RepeatMode.Restart
    )
)

// float
val floatOffset by rememberInfiniteTransition().animateFloat(
    initialValue = 0f, targetValue = -10f,
    animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = EaseInOut),
        repeatMode = RepeatMode.Reverse
    )
)
```

**iOS (SwiftUI):**

```swift
// pixelBlink
struct PixelBlink: ViewModifier {
    @State private var visible = true

    func body(content: Content) -> some View {
        content
            .opacity(visible ? 1 : 0)
            .onAppear {
                Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { _ in
                    visible.toggle()
                }
            }
    }
}

// float
struct FloatAnimation: ViewModifier {
    @State private var offset: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .offset(y: offset)
            .onAppear {
                withAnimation(.easeInOut(duration: 3).repeatForever(autoreverses: true)) {
                    offset = -10
                }
            }
    }
}
```

---

## 6. Efeitos Visuais Retro

Efeitos que definem a assinatura visual 8-bit do PixStop.

### 6.1 Scanlines (CRT)

Overlay de linhas horizontais semitransparentes simulando um monitor CRT.

**Especificação:**

- Linhas de 2dp de espessura
- Cor: `rgba(0, 0, 0, 0.03)` (preto 3% opacidade)
- Gap entre linhas: 2dp transparente
- Padrão: linha transparente → linha escura → repete
- Z-index acima do conteúdo, **sem interceptar toques** (`pointerEvents: none`)

**Android (Compose):**

```kotlin
fun Modifier.scanlines() = this.drawWithContent {
    drawContent()
    val lineHeight = 2.dp.toPx()
    var y = 0f
    while (y < size.height) {
        y += lineHeight // transparent gap
        drawRect(
            color = Color.Black.copy(alpha = 0.03f),
            topLeft = Offset(0f, y),
            size = Size(size.width, lineHeight)
        )
        y += lineHeight
    }
}
```

**iOS (SwiftUI):**

```swift
struct Scanlines: View {
    var body: some View {
        Canvas { context, size in
            var y: CGFloat = 0
            let lineHeight: CGFloat = 2
            while y < size.height {
                y += lineHeight
                context.fill(
                    Path(CGRect(x: 0, y: y, width: size.width, height: lineHeight)),
                    with: .color(.black.opacity(0.03))
                )
                y += lineHeight
            }
        }
        .allowsHitTesting(false)
    }
}
```

### 6.2 Grid Pattern (matriz digital)

Grid de linhas ciano finas sobre o background, criando atmosfera de tela digital.

**Especificação:**

- Linhas de 1dp de espessura
- Cor: `rgba(0, 245, 212, 0.03)` (cyan 3% opacidade)
- Espaçamento: 40dp entre linhas (horizontal e vertical)
- Aplicar sobre backgrounds de tela cheia (login, landing, etc.)

### 6.3 Renderização Pixelada

Imagens e sprites devem manter a estética pixelada sem suavização.

**Android:**

```kotlin
// BitmapFactory — sem interpolação
val options = BitmapFactory.Options().apply {
    inScaled = false
}
// Compose — sem filtro de suavização
Image(
    painter = painterResource(id = R.drawable.sprite),
    contentDescription = null,
    filterQuality = FilterQuality.None  // renderização pixelada
)
```

**iOS:**

```swift
// SwiftUI
Image("sprite")
    .interpolation(.none)
    .resizable()

// UIKit
imageView.layer.magnificationFilter = .nearest
imageView.layer.minificationFilter = .nearest
```

### 6.4 Pixel Corner Decorations

Pequenos quadrados coloridos nos cantos de containers/cards. Assinatura visual do PixStop.

**Especificação:**

- 4 quadrados posicionados em cada canto do container
- Tamanho: 2×2dp ou 3×3dp
- Offset: -1dp para fora do limite do container (parcialmente sobre a borda)
- Cor: segue a cor semântica do container (ex: `pix-green` para sucesso, `pix-pink` para erro)

```
  [■]═══════════════════[■]
  ║                       ║
  ║     CONTEÚDO          ║
  ║                       ║
  [■]═══════════════════[■]
```

---

## 7. Componentes

Todos os componentes pixel têm **cantos retos** (sem border-radius). Isso é fundamental para a estética 8-bit.

### 7.1 Botões

#### Primary (btn-pixel-primary)

| Propriedade     | Valor                          |
|-----------------|--------------------------------|
| Background      | `pix-cyan` (`#00F5D4`)         |
| Texto           | `pix-dark` (`#050816`)         |
| Fonte           | Press Start 2P, 10sp, uppercase|
| Letter-spacing  | +2sp                           |
| Borda           | 2dp sólida `pix-cyan`          |
| Sombra          | 4×4dp sólida `#00F5D4`         |
| Padding         | 24dp horizontal × 16dp vertical|
| Corner radius   | **0dp** (cantos retos)         |

**Estados:**

| Estado   | Transformação                                   |
|----------|-------------------------------------------------|
| Default  | Posição normal, sombra 4×4                      |
| Pressed  | Translate +2dp ↘, sombra 2×2                    |
| Released | Translate +4dp ↘, sombra 0 (completo)           |
| Hover*   | Background muda para `pix-green` (`#00E676`), borda `pix-green` |
| Disabled | Opacidade 50%, sem sombra                       |
| Loading  | Ícone muda para spinner animado, texto alternativo |

> *Hover aplica-se a interações com mouse (tablets/Chromebooks). No toque, o efeito pressed já é suficiente.

#### Secondary (btn-pixel-secondary)

| Propriedade     | Valor                          |
|-----------------|--------------------------------|
| Background      | Transparente                   |
| Texto           | `pix-cyan` (`#00F5D4`)         |
| Fonte           | Press Start 2P, 10sp, uppercase|
| Borda           | 2dp sólida `pix-cyan`          |
| Sombra          | 4×4dp `currentColor`           |
| Padding         | 24dp × 16dp                    |
| Corner radius   | **0dp**                        |

**Estado pressed:** Background `pix-cyan` 10% opacidade + translate/sombra como primary.

#### Destructive

| Propriedade     | Valor                          |
|-----------------|--------------------------------|
| Background      | `pix-pink` (`#FF6B9D`)         |
| Texto           | Branco (`#FFFFFF`)             |
| Borda           | 2dp `pix-pink`                 |

#### Tamanhos

| Tamanho  | Altura | Padding H | Padding V | Fonte  |
|----------|--------|-----------|-----------|--------|
| `sm`     | 32dp   | 16dp      | 8dp       | 8sp    |
| `default`| 36dp   | 20dp      | 10dp      | 10sp   |
| `lg`     | 40dp   | 24dp      | 12dp      | 10sp   |
| `icon`   | 36dp   | —         | —         | —      |
| `icon-sm`| 32dp   | —         | —         | —      |
| `icon-lg`| 40dp   | —         | —         | —      |

#### Outras Variantes

| Variante         | Background        | Texto         | Borda           |
|------------------|-------------------|---------------|-----------------|
| `outline`        | Transparente      | `pix-cyan`    | 2dp `pix-cyan` 50% |
| `secondary`      | `pix-purple`      | Branco        | Nenhuma         |
| `ghost`          | Transparente      | `pix-gray-300`| Nenhuma         |
| `link`           | Transparente      | `pix-cyan`    | Nenhuma (underline no texto) |
| `pixel-secondary`| Transparente      | `pix-purple`  | 2dp `pix-purple`|

---

### 7.2 Inputs

#### Input de Texto (PixelInput)

| Propriedade     | Valor                                 |
|-----------------|---------------------------------------|
| Background      | `pix-dark` (`#050816`)                |
| Texto           | `pix-gray-100` (`#E2E8F0`)           |
| Placeholder     | `pix-gray-500` (`#475569`)           |
| Borda           | 2dp sólida `pix-gray-600` (`#334155`)|
| Padding         | 16dp horizontal × 12dp vertical      |
| Corner radius   | **0dp**                               |
| Fonte (valor)   | Inter, 14sp                           |

**Label (acima do input):**

| Propriedade     | Valor                                 |
|-----------------|---------------------------------------|
| Fonte           | Press Start 2P, 8sp                   |
| Cor             | `pix-cyan` (`#00F5D4`)               |
| Margin bottom   | 8dp                                   |
| Transform       | Uppercase (opcional, segue padrão web)|

**Ícone trailing:**

- Posição: lado direito, centralizado verticalmente
- Tamanho: 16dp
- Cor: `pix-gray-400` (`#64748B`)
- Cor no foco: `pix-cyan`

**Estados:**

| Estado    | Borda              | Sombra            | Extra                    |
|-----------|--------------------|--------------------|--------------------------|
| Default   | `pix-gray-600`     | Nenhuma            | —                        |
| Focused   | `pix-cyan`         | `glow-cyan`        | Ícone muda para cyan     |
| Error     | `pix-pink`         | `glow-pink`        | Mensagem de erro abaixo  |
| Readonly  | `pix-gray-700`     | Nenhuma            | Texto `pix-gray-400`, cursor desabilitado |
| Disabled  | `pix-gray-700`     | Nenhuma            | Opacidade 50%            |

**Mensagem de erro:**
- Fonte: Press Start 2P, 6sp
- Cor: `pix-pink` (`#FF6B9D`)
- Margin top: 4dp

#### Input de Senha (PixelPasswordInput)

Mesma especificação do Input de Texto, com adição de:

- Botão toggle visibilidade no lugar do ícone trailing
- Ícone "olho" (`eye`) quando oculto → "olho riscado" (`eye-slash`) quando visível
- Toggle alterna `secureTextEntry` / `inputType`

---

### 7.3 Cards

#### Card Pixel (card-pixel)

| Propriedade       | Valor                                        |
|-------------------|----------------------------------------------|
| Background        | `pix-gray-800` (`#0F172A`) 50% opacidade     |
| Borda             | 2dp sólida `pix-gray-600` (`#334155`)        |
| Sombra            | 4×4dp `rgba(100, 116, 139, 0.3)`             |
| Padding           | 24dp                                         |
| Corner radius     | **0dp**                                      |
| Backdrop blur     | 8dp (se suportado pela plataforma)            |

**Estado ativo/pressionado:**

- Borda muda para `pix-cyan` (`#00F5D4`)
- Sombra muda para `rgba(0, 245, 212, 0.3)`

#### Card de Autenticação (Terminal Style)

Usado em telas de login, registro, etc.

```
 ╔══════════════════════════════════════════╗
 ║ [●] [●] [●]  SYSTEM.AUTH               ║  ← Header bar
 ╠══════════════════════════════════════════╣
 ║                                          ║
 ║  [ícone]  TÍTULO                        ║
 ║           subtítulo                      ║
 ║                                          ║
 ║  LABEL                                  ║
 ║  ┌──────────────────────────── [ico]┐   ║
 ║  │ input value                      │   ║
 ║  └──────────────────────────────────┘   ║
 ║                                          ║
 ║  ╔══════════════════════════════════╗   ║
 ║  ║       BOTÃO PRIMARY             ║   ║
 ║  ╚══════════════════════════════════╝   ║
 ║                                          ║
 ╚══════════════════════════════════════════╝
  [■]                                  [■]    ← Pixel corner decorations
```

| Propriedade       | Valor                              |
|-------------------|------------------------------------|
| Background        | `pix-gray-900` (`#0A0F1A`)         |
| Borda             | 4dp sólida `pix-cyan` (`#00F5D4`)  |
| Sombra            | `glow-cyan` (blur 20dp)            |
| Corner radius     | **0dp**                            |
| Largura           | Tela cheia com padding 16dp lateral|

**Header bar do terminal:**

| Propriedade     | Valor                                        |
|-----------------|----------------------------------------------|
| Background      | `pix-gray-800` (`#0F172A`)                   |
| Padding         | 12dp horizontal × 8dp vertical               |
| Borda inferior  | 1dp `pix-cyan` 20% opacidade                 |

**Pontos coloridos (window controls):**

| Ponto | Cor                      | Tamanho | Gap    |
|-------|--------------------------|---------|--------|
| 1     | `pix-pink` (`#FF6B9D`)   | 3×3dp   | —      |
| 2     | `pix-yellow` (`#FFD700`) | 3×3dp   | 6dp    |
| 3     | `pix-green` (`#00E676`)  | 3×3dp   | 6dp    |

**Texto do header:** `SYSTEM.AUTH` — Press Start 2P, 7sp, `pix-cyan` 60% opacidade

#### Decorações de canto do Auth Card

4 quadrados decorativos nas extremidades externas do card:

| Posição          | Tamanho | Cor          | Offset              |
|------------------|---------|--------------|---------------------|
| Topo-esquerdo    | 4×4dp   | `pix-cyan`   | -6dp, -6dp          |
| Topo-direito     | 3×3dp   | `pix-green`  | -4dp, -4dp          |
| Baixo-esquerdo   | 3×3dp   | `pix-purple` | -4dp, -4dp          |
| Baixo-direito    | 5×5dp   | `pix-yellow` | -8dp, -8dp          |

---

### 7.4 Auth Header (PixelAuthHeader)

Header com ícone colorido e textos usado no topo dos cards de autenticação.

**Estrutura:**

```
  [ícone com glow]
  TÍTULO (font-pixel)
  subtítulo (font-sans)
```

**Ícone container:**

| Propriedade     | Valor                                 |
|-----------------|---------------------------------------|
| Tamanho         | 64×64dp                               |
| Borda           | 4dp sólida, cor do tema               |
| Background      | Cor do tema                           |
| Sombra          | Neon glow da cor do tema              |
| Corner radius   | **0dp**                               |

**Mapeamento de cores por tema:**

| Tema     | Background   | Borda        | Glow          | Texto título |
|----------|-------------|--------------|---------------|-------------|
| `cyan`   | `#00F5D4`   | `#00F5D4`    | `glow-cyan`   | `#00F5D4`   |
| `green`  | `#00E676`   | `#00E676`    | `glow-green`  | `#00E676`   |
| `yellow` | `#FFD700`   | `#FFD700`    | `glow-yellow` | `#FFD700`   |
| `purple` | `#9D4EDD`   | `#9D4EDD`    | glow purple   | `#9D4EDD`   |
| `orange` | `#FF9100`   | `#FF9100`    | glow orange   | `#FF9100`   |
| `blue`   | `#00B4D8`   | `#00B4D8`    | glow blue     | `#00B4D8`   |
| `pink`   | `#FF6B9D`   | `#FF6B9D`    | `glow-pink`   | `#FF6B9D`   |

**Ícone dentro do container:** Cor `pix-dark` (`#050816`) — 24dp

**Título:** Press Start 2P, 14sp, cor do tema
**Subtítulo:** Inter, 14sp, `pix-gray-400` (`#64748B`)
**Gap ícone→título:** 16dp · **Gap título→subtítulo:** 4dp

---

### 7.5 Checkbox (PixelCheckbox)

| Propriedade       | Valor default                          | Valor checked                  |
|-------------------|----------------------------------------|--------------------------------|
| Tamanho           | 20×20dp                                | 20×20dp                        |
| Borda             | 2dp `pix-gray-600`                     | 2dp `pix-cyan`                 |
| Background        | `pix-dark`                             | `pix-cyan`                     |
| Check mark        | Não visível                            | `✓` branco ou `pix-dark`       |
| Corner radius     | **0dp**                                | **0dp**                        |

**Label ao lado:**
- Fonte: Inter, 14sp
- Cor default: `pix-gray-400`
- Cor hover/checked: `pix-cyan`
- Gap checkbox→label: 12dp

---

### 7.6 Badges (badge-pixel)

| Propriedade     | Valor                                 |
|-----------------|---------------------------------------|
| Fonte           | Press Start 2P, 6sp, uppercase        |
| Borda           | 2dp sólida, cor do badge              |
| Sombra          | 2×2dp sólida `currentColor`           |
| Padding         | 12dp horizontal × 8dp vertical        |
| Corner radius   | **0dp**                               |
| Display         | Inline (wrap ao conteúdo)             |

---

### 7.7 Divider (PixelDivider)

Linha horizontal com texto centralizado.

```
  ─────────── OU ───────────
```

| Propriedade     | Valor                                 |
|-----------------|---------------------------------------|
| Linha           | 2dp `pix-gray-700` (`#1E293B`)        |
| Texto           | Press Start 2P, 6sp                   |
| Cor texto       | `pix-gray-500` (`#475569`)            |
| Texto default   | "OU"                                  |
| Background texto| `pix-gray-900` (para "cortar" a linha)|
| Padding texto   | 8dp horizontal                        |

---

### 7.8 Status Message (PixelStatusMessage)

Mensagem de sucesso com fundo verde translúcido.

| Propriedade     | Valor                                   |
|-----------------|---------------------------------------- |
| Background      | `pix-green` (`#00E676`) 20% opacidade   |
| Borda           | 2dp `pix-green`                         |
| Texto           | Press Start 2P, 7sp, `pix-green`       |
| Ícone           | Check circle, `pix-green`, 16dp        |
| Padding         | 12dp                                    |
| Corner radius   | **0dp**                                 |

---

### 7.9 Flash Messages / Toasts

Notificações temporárias fixas na parte inferior da tela.

| Propriedade     | Sucesso                          | Erro                             |
|-----------------|----------------------------------|----------------------------------|
| Posição         | Bottom 80dp (mobile)             | Bottom 80dp                      |
| Background      | `pix-dark` (`#050816`)           | `pix-dark`                       |
| Borda           | 4dp `pix-green` (`#00E676`)      | 4dp `pix-pink` (`#FF6B9D`)      |
| Sombra          | `glow-green`                     | `glow-pink`                      |
| Texto           | Press Start 2P, 11sp            | Press Start 2P, 11sp            |
| Cor texto       | `pix-green`                      | `pix-pink`                       |
| Corner radius   | **0dp**                          | **0dp**                          |
| Largura         | Tela cheia - 32dp padding lateral| Mesmo                            |
| Auto-dismiss    | 4000ms                           | 5000ms                           |
| Animação entrada| `slideUp` (500ms)                | `slideUp`                        |
| Pixel corners   | 4× quadrados 2dp `pix-green`    | 4× quadrados 2dp `pix-pink`     |

---

### 7.10 Dialogs / Modais

| Propriedade     | Valor                                 |
|-----------------|---------------------------------------|
| Overlay         | Preto 80% opacidade                   |
| Background      | `pix-gray-800` (`#0F172A`)            |
| Borda           | 1dp `pix-cyan` 20% opacidade          |
| Sombra          | `pixel-sm` (2×2dp)                    |
| Corner radius   | **0dp**                               |
| Padding         | 24dp                                  |
| Animação entrada| Zoom 0.95→1.0 + fade in (200ms)       |
| Animação saída  | Zoom 1.0→0.95 + fade out (150ms)      |
| Largura         | Tela cheia - 32dp lateral (mobile)     |

---

### 7.11 Step Indicators

Indicadores de etapa para fluxos multi-step (ex: registro de empresa).

```
  (1)───(2)───(3)
```

**Círculo/quadrado de step:**

| Propriedade     | Ativo                             | Inativo                          |
|-----------------|-----------------------------------|----------------------------------|
| Tamanho         | 32×32dp                           | 32×32dp                          |
| Background      | `pix-green` (`#00E676`)           | Transparente                     |
| Borda           | 2dp `pix-green`                   | 2dp `pix-gray-600`              |
| Texto (número)  | `pix-dark`, Press Start 2P, 10sp  | `pix-gray-500`, Press Start 2P   |
| Corner radius   | **0dp** (quadrado)                | **0dp**                          |

**Conector entre steps:**

| Propriedade     | Completo            | Incompleto           |
|-----------------|---------------------|----------------------|
| Tamanho         | 32dp × 4dp          | 32dp × 4dp           |
| Background      | `pix-green`         | `pix-gray-600`       |

---

### 7.12 Submit Button (PixelSubmitButton)

Variação do botão primary com estados de loading.

| Estado        | Ícone                     | Texto                   |
|---------------|---------------------------|-------------------------|
| Idle          | Ícone custom (FontAwesome)| Texto `idleText`        |
| Processing    | Spinner animado (rotate)  | Texto `processingText`  |

- Durante processing: botão desabilitado, opacidade reduzida
- Estilo base: `btn-pixel-primary`

---

## 8. Ícones

### 8.1 Biblioteca de Referência

O web usa **FontAwesome 7** (Solid, Regular, Brands). Para o app mobile:

**Opção 1 — FontAwesome nativo:** Usar o SDK do FontAwesome para Android/iOS.

**Opção 2 — Ícones nativos:** Mapear para equivalentes nas plataformas:

| FA Icon (web)          | SF Symbols (iOS)           | Material Icons (Android)    | Uso              |
|------------------------|----------------------------|-----------------------------|------------------|
| `gamepad`              | `gamecontroller.fill`      | `sports_esports`            | Dashboard        |
| `building`             | `building.2.fill`          | `business`                  | Empresa          |
| `user-plus`            | `person.badge.plus`        | `person_add`                | Registro         |
| `right-to-bracket`     | `arrow.right.to.line`      | `login`                     | Login            |
| `play`                 | `play.fill`                | `play_arrow`                | CTA              |
| `shield-halved`        | `shield.lefthalf.filled`   | `security`                  | Segurança        |
| `bolt`                 | `bolt.fill`                | `flash_on`                  | Velocidade       |
| `envelope`             | `envelope.fill`            | `email`                     | E-mail input     |
| `lock`                 | `lock.fill`                | `lock`                      | Senha input      |
| `eye` / `eye-slash`    | `eye` / `eye.slash`        | `visibility` / `visibility_off` | Toggle senha |
| `spinner`              | `progress.indicator`       | `CircularProgressIndicator` | Loading          |
| `circle-check`         | `checkmark.circle.fill`    | `check_circle`              | Sucesso          |
| `right-to-bracket`     | `arrow.right.to.line`      | `login`                     | Entrar           |
| `circle-xmark`         | `xmark.circle.fill`        | `cancel`                    | Fechar/Erro      |
| `gear`                 | `gearshape.fill`           | `settings`                  | Configurações    |
| `arrow-right-from-bracket` | `arrow.forward.square` | `logout`                    | Logout           |

### 8.2 Estilo dos Ícones

- Tamanho padrão em componentes: 16dp
- Tamanho em containers de ícone (`icon-pixel`): 24dp
- Cor: herda do contexto (geralmente `pix-gray-400` ou cor accent)
- Renderização: Sólida (filled), não outline — consistente com FA Solid

---

## 9. Espaçamentos

### 9.1 Tokens de Espaçamento

| Token          | Valor | Uso                                    |
|----------------|-------|----------------------------------------|
| `space-xs`     | 4dp   | Gaps mínimos, margins de erro          |
| `space-sm`     | 8dp   | Gap texto-label, padding de badges     |
| `space-md`     | 12dp  | Padding de inputs (vertical)           |
| `space-base`   | 16dp  | Padding de inputs (horizontal), gap grid |
| `space-lg`     | 20dp  | Gap entre campos de formulário         |
| `space-xl`     | 24dp  | Padding de cards, padding horizontal de seções |
| `space-2xl`    | 32dp  | Gap entre seções, padding expandido de cards |
| `space-3xl`    | 48dp  | Margem de separação entre blocos       |
| `space-4xl`    | 64dp  | Padding vertical de seções             |

### 9.2 Aplicação

| Contexto                   | Padding/Margin                          |
|----------------------------|-----------------------------------------|
| Tela (safe area)           | 16dp horizontal                         |
| Seção principal            | 24dp horizontal × 64dp vertical         |
| Card padrão                | 24dp (todos os lados)                   |
| Card expandido             | 32dp (todos os lados)                   |
| Input (interno)            | 16dp horizontal × 12dp vertical         |
| Botão pixel                | 24dp horizontal × 16dp vertical         |
| Botão compacto             | 20dp horizontal × 8dp vertical          |
| Gap campos de formulário   | 20dp vertical                           |
| Gap entre seções           | 32dp vertical                           |
| Gap em grids/listas        | 16dp ou 24dp                            |
| Label → input              | 8dp                                     |
| Input → mensagem de erro   | 4dp                                     |
| Ícone → texto (em linha)   | 8dp                                     |

### 9.3 Cantos

**Regra fundamental:** Componentes pixel (botões, inputs, cards, badges, checkboxes) **NÃO usam border-radius**. Cantos retos são essenciais para a estética 8-bit.

Apenas componentes de sistema (bottom sheets nativos, status bar, etc.) podem manter border-radius da plataforma.

---

## 10. Navegação

### 10.1 Sidebar → Bottom Navigation / Drawer

O padrão de sidebar do web deve ser traduzido para navegação mobile adequada.

**Opção recomendada: Bottom Navigation Bar**

| Propriedade     | Valor                                   |
|-----------------|-----------------------------------------|
| Background      | `pix-darker` (`#030510`)               |
| Borda superior  | 1dp `pix-gray-700` (`#1E293B`)         |
| Altura          | 56dp + safe area bottom                 |
| Sombra          | Nenhuma (borda já delimita)             |

**Item de navegação:**

| Estado   | Ícone               | Label                              |
|----------|---------------------|------------------------------------|
| Inativo  | `pix-gray-400`      | Press Start 2P, 7sp, `pix-gray-500`, uppercase |
| Ativo    | `pix-cyan`          | Press Start 2P, 7sp, `pix-cyan`, uppercase |

**Background do item ativo:** `pix-cyan` 10% opacidade (`rgba(0, 245, 212, 0.1)`)

### 10.2 Drawer (alternativa para mais itens)

Se houver muitos itens de navegação:

| Propriedade     | Valor                                   |
|-----------------|-----------------------------------------|
| Background      | `pix-darker` (`#030510`)               |
| Largura         | 280dp                                  |
| Header          | Logo PixStop + nome "PixStop" em `font-pixel` |
| Borda direita   | 1dp `pix-gray-700`                     |

**Grupo de itens:**

| Propriedade     | Valor                                   |
|-----------------|-----------------------------------------|
| Label do grupo  | Press Start 2P, 8sp, `pix-cyan` 60%, uppercase, tracking +2 |
| Item normal     | Inter, 14sp, `pix-gray-200`            |
| Item ativo      | Inter, 14sp, `pix-cyan`, background `pix-cyan` 10% |
| Ícone           | 20dp, mesma cor do texto                |
| Padding item    | 16dp horizontal × 12dp vertical         |
| Gap ícone→texto | 12dp                                    |

### 10.3 Top App Bar

| Propriedade     | Valor                                   |
|-----------------|-----------------------------------------|
| Background      | `pix-dark` (`#050816`)                  |
| Título          | Press Start 2P, 12sp, `pix-cyan`       |
| Borda inferior  | 1dp `pix-cyan` 20% opacidade            |
| Ícones          | `pix-gray-300`, 24dp                    |
| Altura          | 56dp + status bar                       |

---

## 11. Referências

### Fontes

- [Press Start 2P — Google Fonts](https://fonts.google.com/specimen/Press+Start+2P)
- [Inter — Google Fonts](https://fonts.google.com/specimen/Inter)

### Documentação PixStop

- [Componentes Pixel Web](../resources/js/components/pixel/PIXEL-COMPONENTS.md)
- [CSS Theme Source](../resources/css/app.css)
- [API Mobile](MOBILE_API.md)

### Plataforma

- [Material Design 3](https://m3.material.io)
- [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines)
- [FontAwesome](https://fontawesome.com)
- [SF Symbols](https://developer.apple.com/sf-symbols/)

---

## Apêndice: Checklist de Implementação

- [ ] Configurar cores (`PixColors` / `Color+Pix`)
- [ ] Importar fontes Press Start 2P e Inter
- [ ] Criar modifier/extension `pixelShadow`
- [ ] Criar modifier/extension `neonGlow`
- [ ] Criar overlay de scanlines
- [ ] Criar componente `PixelButton` (primary, secondary, destructive)
- [ ] Criar componente `PixelInput` com label e estados
- [ ] Criar componente `PixelPasswordInput` com toggle
- [ ] Criar componente `PixelCard` (padrão e terminal)
- [ ] Criar componente `PixelCheckbox`
- [ ] Criar componente `PixelBadge`
- [ ] Criar componente `PixelDivider`
- [ ] Criar componente `PixelToast` (sucesso e erro)
- [ ] Criar componente `PixelDialog`
- [ ] Criar componente `PixelStepIndicator`
- [ ] Criar `BottomNavBar` / `Drawer` com estilo PixStop
- [ ] Criar `TopAppBar` com estilo PixStop
- [ ] Implementar animações (blink, float, glitch, slideUp)
- [ ] Reproduzir logo "P" pixel-art
- [ ] Validar cantos retos em todos os componentes pixel

