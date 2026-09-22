# Publicação nas lojas — Play Store e App Store

> Data: 2026-09-19
> Primeira publicação. Nada foi enviado a nenhuma das lojas ainda.

Roteiro para levar o Pixelstop ao Google Play e à App Store, montado a partir do
que foi feito nos apps irmãos. O detalhe de cada armadilha está lá:

| Assunto | Referência no immo-vistoria-mobile | No mobile-immo |
|---|---|---|
| Keystore, versão, AAB | `docs/PUBLICACAO-PLAY-STORE.md` | `docs/15-PUBLICACAO-PLAY-STORE.md` |
| App Store | `docs/PUBLICACAO-APP-STORE.md` | `docs/32-PUBLICACAO-APP-STORE.md` |
| Capturas | `docs/CAPTURAS-DE-LOJA.md` + `docs/vitrine/` (scripts) | `docs/28-CAPTURAS-DE-LOJA.md` |
| Textos da ficha | `docs/FICHAS-DE-LOJA.md` | `docs/29-FICHAS-DE-LOJA.md` |
| Segurança dos dados | `docs/PRIVACIDADE-E-DADOS.md` | `docs/30-PRIVACIDADE-E-DADOS.md` |
| Resposta ao App Review | `docs/APP-REVIEW-NOTAS.md` | — |

O estado abaixo foi levantado do código em 19/09/2026. O branch
`feat/geladeira-bluetooth`, com todo o rebrand Pixelstop, foi integrado ao
`main` no mesmo dia — o build de loja sai do `main`.

| Item | Valor hoje |
|---|---|
| applicationId / Bundle ID | `com.pixstop.mobile` (ver decisão 1) |
| Versão | `0.1.0` (versionCode 1), igual no `gradle.properties` e nos `xcconfig` (22/09) |
| Nome no aparelho | "Pixelstop" nas duas plataformas |
| API de produção | `https://pixelstop.com.br/api` (corrigida em 19/09, ver bloqueador 1) |
| Ambientes | `local` e `production`; o `staging` saiu em 22/09 (host não resolve) |
| Assinatura Android | `keystore.properties` não existe, a chave não foi criada |
| Time Apple | `DV6WLH3JBP` (IMMO Tecnologia) nos `xcconfig` — build de aparelho assina (22/09) |

---

## Decisões antes do primeiro envio

Tudo aqui é barato agora e caro ou impossível depois do primeiro upload.

### 1. O identificador do app

`com.pixstop.mobile` é permanente nas duas lojas: não muda depois do primeiro
upload, e aparece na URL pública da Play
(`play.google.com/store/apps/details?id=com.pixstop.mobile`). Com a marca agora
Pixelstop, **a recomendação é trocar para `com.pixelstop.mobile` antes do
primeiro envio**. Pontos a trocar juntos: `applicationId` em
`androidApp/build.gradle.kts`, `PRODUCT_BUNDLE_IDENTIFIER` nos três `xcconfig`
e o `KEYCHAIN_SERVICE` do `PlatformModule.ios.kt`. O esquema de deep link
(`pixstop://`) pode ficar — é interno.

### 2. De quem é a conta de desenvolvedor

Os apps da Immo e do VistoMais estão na conta da **IMMO Tecnologia e Soluções
Digitais Ltda.** (time Apple `DV6WLH3JBP`). Publicar o Pixelstop ali faz a loja
exibir "IMMO Tecnologia" como desenvolvedor. Se o Pixelstop for outra empresa,
é outra conta — e a da Apple, para pessoa jurídica, pede D-U-N-S e leva dias.
A política de privacidade precisa identificar **a mesma** empresa que aparece
como desenvolvedora.

### 3. Android primeiro

Recomendação: publicar no Play antes. No iOS três recursos ainda não existem —
abrir a geladeira por Bluetooth (B3), entrar sozinho na rede da geladeira
(`NEHotspotConfiguration`) e escolher foto da galeria para o avatar
(`PhotoPicker.ios.kt`). O app funciona sem eles, mas a ficha da Apple não pode
prometê-los, e o revisor da Apple é mais rigoroso com app que depende de
hardware.

---

## Bloqueadores — impedem o envio

### 1. O app de produção falava com o servidor antigo — resolvido em 19/09/2026

`androidApp/build.gradle.kts` (flavor `production`) e `composeApp/build.gradle.kts`
(`baseUrl`) apontavam para `https://pixstop.com.br/api`. Esse domínio resolve para
**44.208.193.35**, a produção single-tenant antiga; o multi-tenant está em
`pixelstop.com.br` (**32.196.33.37**). Conferido em 19/09:

| URL | Resposta |
|---|---|
| `https://pixstop.com.br/api/legal/documents` | 404 (servidor antigo, não tem a rota) |
| `https://pixelstop.com.br/api/legal/documents` | 401 JSON do app — é este |

Trocado para `https://pixelstop.com.br/api` nos dois arquivos; o `BuildConfig` e
o `BuildKonfig` do `productionRelease` saem com a URL nova. Os leitores de link
(`CompanyCodeParser`, `NotificationRouter`) não conferem o domínio, então QR
Codes e avisos do servidor novo funcionam sem mudança.

Continua em aberto: o staging (`staging.pixstop.com.br`) não responde — em
22/09 o ambiente foi removido do app, que agora tem só `local` e `production`.

### 2. Keystore de release

Não existe. Criar uma vez e guardar com backup fora do repositório — perder a
chave impede atualizar o app:

```bash
keytool -genkey -v -keystore ~/keystore/pixelstop-release.jks \
  -alias pixelstop -keyalg RSA -keysize 2048 -validity 10000
cp keystore.properties.example keystore.properties   # e preencher
```

O `build.gradle.kts` já lê o arquivo; sem ele, o release sai com a chave de
debug e a Play recusa. Ativar a **Assinatura de apps do Google Play** no
primeiro upload (o Google guarda a chave de assinatura; a local vira chave de
upload).

### 3. Política de privacidade desatualizada

`https://pixelstop.com.br/privacidade` responde 200, mas o texto (versão 1.0.0):

- chama a empresa de **"Pixstop"**, não Pixelstop;
- **não identifica o controlador**: sem razão social, CNPJ nem endereço — a
  LGPD exige, e o revisor procura;
- dá como contato do encarregado `privacidade@pixstop.com.br` — o domínio tem MX
  (Hostinger), mas confirmar que a caixa existe e alguém lê;
- não fala do que o **app** faz: câmera para QR Code, foto para o avatar
  gerado por IA, cartão tokenizado pelo Mercado Pago, Bluetooth para abrir a
  geladeira. Declaração que não bate com o app é motivo de reprovação no Play.

Editar em **/admin/legal** (tem versionamento) e publicar a 1.1.0 antes de
cadastrar a URL. O mesmo vale para os Termos (`/termos`), que também dizem
"Pixstop".

A página é Inertia — o texto só aparece com JavaScript. Abre no Chrome do
Android, mas no immo-vistoria a lição foi servir **HTML puro** para as URLs
cadastradas nas lojas (`LegalHtmlPageController`). Vale considerar o mesmo aqui.

### 4. Exclusão de conta — contato de suporte

A exclusão dentro do app existe (Perfil › Excluir conta, com senha, apaga em 90
dias) e a página pública `https://pixelstop.com.br/exclusao-de-conta` responde.
Mas a página mostra como contato `nao-responda@pixelstop.com.br` —
o `supportEmail` vem de `config('mail.from.address')` em
`AccountDeletionController`. Um endereço "não responda" como canal de exclusão é
exatamente o que o revisor testa. Criar uma config própria de suporte
(`suporte@pixelstop.com.br`) no backend.

### 5. Conta de revisão

O app não faz nada sem **empresa vinculada** (o fluxo cai em `JoinCompanyScreen`)
e a compra só fecha diante de uma geladeira. Criar no tenant de produção:

- `review@pixelstop.com.br` — usuário comum, com empresa, saldo de pixels,
  pedidos no histórico e produtos com estoque na vitrine;
- `review.admin@pixelstop.com.br` — administrador da empresa (Equipe, distribuir
  pixels, operador).

O backend **não tem** o equivalente ao `review:provision` do immo-vistoria —
criar à mão ou portar o comando. Nunca commitar a senha.

Para a abertura física da geladeira, que o revisor não consegue testar, gravar
**vídeo** do fluxo completo diante da geladeira real e colocar o link nas notas
de revisão das duas lojas.

---

## Play Store

### Checklist

- [ ] Decisões 1 e 2 tomadas
- [ ] Bloqueadores 1–5 resolvidos
- [x] `feat/geladeira-bluetooth` integrado ao `main` (19/09)
- [ ] Versão `1.0.0` (versionCode 1) no `gradle.properties`
- [ ] `./gradlew :androidApp:bundleProductionRelease` assinado e testado num aparelho
- [ ] Ícone 512×512 e gráfico de destaque 1024×500
- [ ] 2 a 8 capturas (roteiro abaixo)
- [ ] Ficha, Segurança dos dados, classificação de conteúdo, público-alvo
- [ ] Teste interno → teste fechado (ver abaixo) → produção

### Build

```bash
JAVA_HOME=/home/diego/.jdks/jbr-21.0.11 ./gradlew :androidApp:bundleProductionRelease
# saída: androidApp/build/outputs/bundle/productionRelease/androidApp-production-release.aab
```

O release roda com R8 (`isMinifyEnabled` + `isShrinkResources`). **Instalar e
exercitar o release antes de subir** — no mobile-immo a primeira ativação do R8
exigiu regras novas no `proguard-rules.pro`. Pontos sensíveis aqui: DTOs do
kotlinx.serialization, Koin, Ktor e o conector Bluetooth. Fluxo mínimo: login,
vitrine, carrinho, Pix, cartão, abrir geladeira pelo rádio, avatar.

### Teste fechado obrigatório

Conta pessoal criada depois de novembro de 2023 precisa de **teste fechado com
12 testadores por 14 dias seguidos** antes de liberar produção. Conta de
organização (CNPJ) está isenta. Mais um motivo para decidir a conta (decisão 2)
já.

### Versionamento

Uma fonte só: `gradle.properties` (`app.versionName` / `app.versionCode`). No
mesmo commit, atualizar `MARKETING_VERSION` e `CURRENT_PROJECT_VERSION` nos
`xcconfig`. `versionCode` nunca repete — no mobile-immo o 12 foi queimado num
upload e a versão seguinte teve de ser 13. Taguear `v1.0.0` após o upload.

### Segurança dos dados — respostas

Levantado do código, não do que o app "poderia" coletar. Confirmar antes de
enviar.

| Tipo de dado | Coletado | Compartilhado | Obrigatório | Finalidade | Evidência |
|---|---|---|---|---|---|
| Nome | Sim | Não | Sim | Funcionalidade, conta | `RegisterScreen` |
| E-mail | Sim | Não | Sim | Funcionalidade, conta | `RegisterScreen` |
| Informações de pagamento | Sim | Não¹ | Não | Funcionalidade | `CardForm`, `CardTokenizer` |
| Histórico de compras | Sim | Não | Sim | Funcionalidade | pedidos, `PixelHistoryScreen` |
| Fotos | Sim | Não¹ | Não | Funcionalidade (avatar por IA) | `AvatarRepository.generatePixel/upload` |
| Outros conteúdos do usuário | Verificar | — | — | — | nome da equipe, convites |

¹ Mercado Pago e o provedor de IA tratam os dados em nome do desenvolvedor — na
definição do Google, isso não é compartilhamento. Mesma leitura do
immo-vistoria; confirmar com o jurídico.

**Não declarar**: localização (a `ACCESS_FINE_LOCATION` só existe até o
Android 11, exigida pelo sistema para escanear Bluetooth, e nada sai do
aparelho), contatos, áudio, identificadores de publicidade, analytics, registros
de falha (o app **não tem Firebase nem Crashlytics**).

Práticas: criptografia em trânsito **sim** (a produção bloqueia HTTP —
`network_security_config_production`); exclusão a pedido **sim**, pelo app e
pela URL.

**Declaração de permissões**: o Play pergunta pela localização. Resposta: usada
apenas no Android ≤ 11, onde o sistema a exige para buscar a geladeira por
Bluetooth; no 12+ o app usa `BLUETOOTH_SCAN` com `neverForLocation`.

### URLs

| Campo | URL | Situação |
|---|---|---|
| Política de privacidade | `https://pixelstop.com.br/privacidade` | No ar, texto a corrigir (bloqueador 3) |
| Exclusão de conta | `https://pixelstop.com.br/exclusao-de-conta` | No ar, contato a corrigir (bloqueador 4) |
| Termos | `https://pixelstop.com.br/termos` | No ar, diz "Pixstop" |

---

## App Store

### Pendências no projeto iOS

> **O iOS compilou e rodou pela primeira vez em 22/09/2026.** Até então nada
> tinha sido construído para a plataforma, e o que estava no `main` não
> compilava. O que foi consertado está no bloco "Primeira compilação" abaixo.

| # | O quê | Onde |
|---|---|---|
| 1 | ~~Nome: "Pixstop"/"PixStop" → Pixelstop~~ — resolvido em 19/09 | `Info.plist`, `PRODUCT_NAME` nos `xcconfig` |
| 2 | ~~`TEAM_ID` vazio~~ — resolvido em 22/09: `DV6WLH3JBP` (IMMO Tecnologia), o mesmo time dos apps Immo e VistoMais | `Config.xcconfig` e `Config-Production.xcconfig` |
| 3 | ~~Só iPhone: `TARGETED_DEVICE_FAMILY = "1,2"` → `1`~~ — resolvido em 22/09 | `project.pbxproj` |
| 4 | ~~Ícone com canal alfa~~ — resolvido em 22/09: o alfa era todo opaco, então nenhum pixel mudou; só o canal saiu | `AppIcon.appiconset/icon-1024.png`, agora RGB |
| 5 | ~~`ITSAppUsesNonExemptEncryption = false` ausente~~ — resolvido em 22/09 | `Info.plist` |
| 6 | ~~`PrivacyInfo.xcprivacy` não existe~~ — criado em 22/09 com `CA92.1` (NSUserDefaults) e os tipos de dado coletados | `iosApp/iosApp/PrivacyInfo.xcprivacy` |
| 7 | ~~Texto da câmera~~ — resolvido em 19/09: convite da empresa e check-in na geladeira. Incluir a foto do avatar quando ela chegar ao iOS | `NSCameraUsageDescription` |
| 8 | ~~Versão `1.0 (1)` diferente do Android~~ — alinhada em 22/09: os `xcconfig` passam a espelhar o `gradle.properties` (`0.1.0`/1). Subir para `1.0.0` é o commit de release | `Config.xcconfig`, `Config-Production.xcconfig` |

### Primeira compilação do iOS — 22/09/2026

O `main` não compilava para iOS. Como `SavedState` é um alias de `Bundle` no
Android e uma classe própria no iOS, e como só o Android tinha sido construído,
nada disso aparecia. Consertado:

| O quê | Onde |
|---|---|
| `entry.arguments?.getString(...)` não existe fora do Android | `AppNavigation.kt` → `read { getStringOrNull(...) }` de `androidx.savedstate` |
| `import` no meio do arquivo, depois de uma anotação | `PlatformModule.ios.kt` |
| `PixColors` (getter `@Composable`) lido de dentro de `drawBehind` | `QrCodeScanner.ios.kt`, cor içada para fora |
| 47 nomes de teste com vírgula — o Kotlin/Native recusa, o JVM aceita | 22 arquivos em `commonTest` |
| `PRODUCT_BUNDLE_IDENTIFIER=com.pixstop.mobile$(TEAM_ID)`: preencher o time daria `com.pixstop.mobileDV6WLH3JBP`. O `$(TEAM_ID)` saiu do bundle ID e ficou só no `DEVELOPMENT_TEAM` | `Config.xcconfig` |
| Faltava `NSLocalNetworkUsageDescription` — sem ela o iOS 14+ bloqueia o portal da geladeira em `http://10.42.0.1`, e a Fase 9.3 não funciona | `Info.plist` |
| **O Koin nunca era iniciado no iOS**: o app abria e fechava em `KoinApplication has not been started` | `startApp()` em `MainViewController.kt`, chamado pelo `init()` do `iOSApp.swift` |
| O Coil não tinha buscador HTTP no iOS — foto de perfil e de produto não apareceriam, sem erro na tela | mesmo `startApp()`, `SingletonImageLoader.setSafe` com `KtorNetworkFetcherFactory` |
| O ambiente `local` mandava o iOS para `10.0.2.2`, endereço que só existe no emulador do Android | `composeApp/build.gradle.kts`, `targetConfigs` do BuildKonfig → `localhost:8010` no iOS |

Conferido no simulador iPhone 17 Pro (Xcode 26.2): `** BUILD SUCCEEDED **`, app
aberto na tela de login contra `https://pixelstop.com.br/api`, e os **278 testes
do `commonTest` passando em `iosSimulatorArm64Test`**.

Com o `TEAM_ID` preenchido, o build para **aparelho real** também passa, assinado
com "Apple Development: Diego Tracz", perfil "iOS Team Provisioning Profile: *",
`Identifier=com.pixstop.mobile`, `TeamIdentifier=DV6WLH3JBP`. Ou seja: dá para
instalar num iPhone e arquivar.

### O que o iOS ainda não faz

| O quê | Situação |
|---|---|
| Abrir a geladeira por Bluetooth (B3 do [MQTT_BLE.md](../../pixstop/docs/plans/MQTT_BLE.md)) | **Não existe.** `UnavailableFridgeUnlockConnector`, `isSupported = false`. O Android (B2) está pronto e provado contra o Pi Zero 2 W. Falta portar para CoreBluetooth. Quando entrar, some `NSBluetoothAlwaysUsageDescription` — sem ela o app **fecha** ao tocar no rádio |
| Entrar sozinho na rede da geladeira | `ManualFridgeNetworkConnector`: a tela diz "Já estou na rede da geladeira" e a pessoa entra pelas configurações. O automático exige `NEHotspotConfiguration`, que é um entitlement à parte |
| Escolher foto da galeria para o avatar | `PhotoPicker.ios.kt` |

O **resto da configuração da geladeira (Fase 9.3) funciona no iOS**: depois de
entrar na rede à mão, `SetupPortalClient` fala com `http://10.42.0.1` como no
Android — lista redes, envia WiFi e código, acompanha o status. É código de
`commonMain`; só o passo de entrar na rede é manual.

O ambiente **staging** foi removido (Android e iOS): `staging.pixstop.com.br`
não resolve, e o multi-tenant responde em `pixelstop.com.br`. Restam `local` e
`production`.

Quando o Bluetooth chegar ao iOS (B3), entra `NSBluetoothAlwaysUsageDescription`
— sem ela o app **fecha** ao tocar no rádio. Quando entrar a galeria, o
`PHPickerViewController` dispensa permissão.

### Diretriz 3.1 — pagamentos

O Pixelstop vende **produto físico** consumido fora do app (a bebida da
geladeira), o que a diretriz 3.1.3(e)/3.1.5(a) libera de compra dentro do app:
Pix e cartão pelo Mercado Pago são permitidos. Três cuidados:

- **Pixels** são saldo que a empresa distribui, não se compram no app — manter
  assim no iOS. Vender pixels no iPhone seria moeda digital, e aí vale a 3.1.1.
- **Assinatura da empresa** (plano do Pixelstop): o app só mostra o estado. Não
  pode haver preço de plano nem link para pagar a assinatura no iOS. O
  immo-vistoria resolveu com um sinalizador `externalPurchaseAllowed` falso no
  iOS (`core/platform/StorePolicy.kt`).
- Nas notas de revisão, dizer explicitamente que se trata de bens físicos.

### Diretriz 3.2 e 2.1 — app para empresas

O app exige código de empresa, o que lembra "app para organização específica".
A resposta, como no immo-vistoria: qualquer empresa pode contratar e qualquer
pessoa se cadastra; mostrar o cadastro no vídeo. As contas de revisão
(bloqueador 5) e o vídeo diante da geladeira resolvem o 2.1.

### Sequência no Mac

Levar o clone e o acesso à conta Apple. Não há `GoogleService-Info.plist` a
copiar — o app não usa Firebase.

1. Abrir `iosApp/iosApp.xcodeproj`, assinar com o time.
2. Build Release com `ENVIRONMENT=production`, no simulador e num iPhone.
3. Product › Archive › Distribute App › App Store Connect.
4. Criar o app com o bundle da decisão 1; subir capturas, ícone 1024, textos;
   preencher o *App Privacy* com a mesma tabela da Play; informar a conta de
   revisão e colar as notas.

Para checar compilação por linha de comando, usar simulador **nomeado**
(`-destination 'platform=iOS Simulator,name=iPhone 17 Pro'`) — o genérico pede
`x86_64`, que o Kotlin não gera. E `CODE_SIGNING_ALLOWED=NO` só serve para
compilar: sem assinatura o Keychain falha e o app fecha em segundos.

### Notas para o App Review (inglês)

Preencher os colchetes na App Store Connect. Não commitar senha.

```
Pixelstop is a smart-fridge micro-market for workplaces in Brazil. Employees
of companies that host a Pixelstop fridge buy drinks and snacks from it with
the app: they pick products, pay with Pix or card (Mercado Pago), and the app
unlocks the fridge door. All purchases are PHYSICAL GOODS consumed outside the
app. Companies may also grant employees "pixels", a corporate balance used
only to buy those goods; pixels cannot be purchased in the iOS app.

DEMO ACCOUNTS
User:  review@pixelstop.com.br / [senha]
Admin: review.admin@pixelstop.com.br / [senha]
Both are linked to a demo company with products, balance and order history.

HARDWARE
Unlocking requires standing in front of a physical fridge. A video of the full
flow on a real device is here: [link não listado]

Anyone can create an account in the app; companies can sign up at
https://pixelstop.com.br. Account deletion: Profile > Excluir conta.
No analytics, advertising or tracking SDKs.
```

---

## Ficha de loja — rascunho

### Google Play

**Nome (30)**

```
Pixelstop
```

**Descrição curta (80)**

```
Pegue, pague com Pix e saia: a geladeira inteligente da sua empresa
```

**Descrição completa (4.000)**

```
O Pixelstop é o app da geladeira inteligente do seu trabalho. Escolha o que
quer levar, pague pelo celular e a porta abre — sem fila, sem caixa, sem
dinheiro trocado.

COMPRAR É RÁPIDO
• Veja na vitrine só o que tem na geladeira, com preço e estoque
• Monte o carrinho e pague com Pix ou cartão
• A porta abre pelo app; pegue os produtos e pronto
• Guarde o cartão para a próxima compra, se quiser

PIXELS
• A sua empresa pode dar pixels para você gastar na geladeira
• Acompanhe o saldo, o que entrou, o que saiu e o que vence
• Combine pixels e Pix no mesmo pedido

SEU HISTÓRICO
• Todos os pedidos, com itens e valores
• Avisos quando o pedido é confirmado

PARA QUEM ADMINISTRA
• Convide a equipe e distribua pixels
• Acompanhe as aberturas e o estoque da geladeira

Para usar é preciso fazer parte de uma empresa que tenha Pixelstop. Peça o
código de convite ao administrador da sua empresa.
```

Revisar contra o app antes de publicar: cada item acima precisa existir no
build que vai para a loja. Não citar abertura por Bluetooth sem rede sem ter
testado no release.

### App Store

| Campo | Texto |
|---|---|
| Nome (30) | `Pixelstop` |
| Subtítulo (30) | `Geladeira inteligente no trabalho` |
| Palavras-chave (100) | `geladeira,vending,pix,lanche,bebida,empresa,copa,micro market,autoatendimento,snack` |
| Texto promocional (170) | `Escolha, pague com Pix e a porta abre. A geladeira da sua empresa, sem fila e sem caixa.` |
| Descrição | A da Play, **sem** o que o iOS ainda não faz (Bluetooth, foto da galeria) |

O nome é único na loja inteira — conferir se "Pixelstop" está livre nas duas
antes de fixar a marca em material impresso.

---

## Capturas

Mesmo método do immo-vistoria (`CAPTURAS-DE-LOJA.md`, 8 etapas). Resumo e o que
muda aqui.

### 1. Tenant de vitrine

Nunca dados da Locarmais nem de outro cliente real. Criar no backend local
(`/home/diego/Projetos/pixstop`, Sail na porta **8010**) um tenant de
demonstração com:

- produtos de verdade, com foto própria ou licenciada — **não** imagem de banco
  sem licença nem marca de terceiros em destaque;
- estoque variado (a vitrine só mostra o que tem estoque — commit `deea80a`);
- pedidos em estados diferentes, saldo de pixels com entradas e saídas;
- nomes e telefones obviamente fictícios.

Versionar os scripts em `docs/vitrine/` deste repositório, como no
immo-vistoria, para refazer no Mac.

### 2. Rota até o backend

IP da rede local no `NGROK_URL` do `local.properties` (com `/api`) e no
`APP_URL` do backend. Conferir o IP a cada sessão. Reverter os dois ao terminar.

### 3. Capturar (Android, aparelho real)

```bash
JAVA_HOME=/home/diego/.jdks/jbr-21.0.11 ./gradlew :androidApp:installLocalDebug
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb exec-out screencap -p > tela.png
```

O MIUI derruba o demo mode quando o app volta ao foco — reaplicar a cada tela.
Captura de ~15 KB é tela preta. Login à mão (automatizado não funciona). Os
scripts `capturar-android.sh`, `ui-android.sh` e `cortar.py` do
immo-vistoria-mobile servem como estão.

### 4. Telas candidatas (escolher até 8)

| Ordem | Tela | Por que |
|---|---|---|
| 1 | `HomeScreen` | primeira impressão, saldo e atalhos |
| 2 | `ShopScreen` | a vitrine da geladeira |
| 3 | `ProductDetailScreen` | produto com foto e preço |
| 4 | `CartScreen` | carrinho |
| 5 | `CheckoutScreen` | Pix / cartão / pixels |
| 6 | `OrderScreen` | abrir a geladeira — o momento do produto |
| 7 | `PixelHistoryScreen` / `RewardsScreen` | pixels e recompensas |
| 8 | `ProgressScreen` com avatar | diferencial visual |

Ficam fora da ficha: telas de operador e de configuração da geladeira.

### 5. Cortar e conferir

Android: cortar para **2:1 ou menos** (1080×2400 → 1080×2160, `cortar.py`) — a
captura crua é recusada no upload. iOS: **não cortar**; capturar no simulador
iPhone 6.9" (1320×2868) e salvar em RGB sem alfa. Montar a folha de contato e
**olhar** antes de publicar — foi assim que se achou um WhatsApp por cima de uma
captura no immo-vistoria.

Destino: `docs/imagens/loja/` e `docs/imagens/loja-ios/`.

### 6. Ícone e destaque

- **Ícone 512×512** (Play) e **1024×1024 sem alfa** (Apple): gerar do vetor da
  marca (`pixstop/public/favicon.svg` ou o original em `public/images/brand/`),
  não de upscale do `ic_launcher`.
- **Destaque 1024×500** (Play): marca sobre a cor da casa, nada importante nas
  bordas.

---

## Ordem sugerida

1. Decisões 1–3.
2. ~~Integrar o branch ao `main` e corrigir a URL~~ — feito em 19/09.
3. Backend: política 1.1.0, termos, e-mail de suporte, contas de revisão.
4. Keystore, build release, teste no aparelho.
5. Vitrine, capturas, ícone e destaque.
6. Criar o app no Play Console, teste interno, (teste fechado se conta pessoal),
   produção.
7. Pendências iOS, Mac, App Store Connect.

## Histórico de versões

| versionCode | versionName | Data | Loja | Descrição |
|---|---|---|---|---|
| 1 | 1.0.0 | — | — | Primeira publicação |
