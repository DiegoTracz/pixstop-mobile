package com.pixstop.mobile.ui.theme

import com.pixstop.mobile.ui.components.AppIconType

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║                    CONFIGURAÇÃO DA MARCA - ALTERE AQUI                     ║
 * ╠════════════════════════════════════════════════════════════════════════════╣
 * ║  Este arquivo centraliza as configurações de branding do aplicativo.       ║
 * ║  Altere os valores abaixo para personalizar o nome e ícone do app.         ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
object AppBranding {

    // ══════════════════════════════════════════════════════════════════════════
    // 📝 NOME DO APLICATIVO
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Nome do aplicativo exibido nas telas.
     *
     * Aparece em:
     * - Splash Screen
     * - Tela de Login
     * - Cabeçalhos
     *
     * ⚠️ IMPORTANTE: Altere também em:
     * - androidApp/src/main/res/values/strings.xml (nome abaixo do ícone)
     * - iosApp/iosApp/Info.plist (CFBundleDisplayName)
     */
    const val APP_NAME = "Pixelstop"

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 ÍCONE/LOGO DO APLICATIVO
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ícone usado como logo do aplicativo.
     *
     * Opções disponíveis (AppIconType):
     * - Lock         🔒 (padrão - ideal para apps de autenticação)
     * - Person       👤 (ideal para apps de perfil/social)
     * - Home         🏠 (ideal para apps de imóveis/casa)
     * - Search       🔍 (ideal para apps de busca)
     * - Notifications 🔔 (ideal para apps de mensagens)
     * - Settings     ⚙️ (ideal para apps de configuração)
     * - Check        ✓ (ideal para apps de tarefas)
     * - Info         ℹ️ (ideal para apps informativos)
     *
     * Para usar um logo personalizado (imagem), veja a documentação em:
     * docs/12-CORES.md seção "Usando Logo Personalizada"
     */
    val APP_ICON: AppIconType = AppIconType.Lock

    // ══════════════════════════════════════════════════════════════════════════
    // 📄 TEXTOS DA TELA DE LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Subtítulo exibido abaixo do nome na tela de login.
     */
    const val LOGIN_SUBTITLE = "Faça login para continuar"
}
