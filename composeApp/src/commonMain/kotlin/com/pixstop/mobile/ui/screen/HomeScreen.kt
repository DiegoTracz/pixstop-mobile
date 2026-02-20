package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.pixstop.mobile.data.model.User
import com.pixstop.mobile.ui.components.AppBottomNavigation
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.BottomNavItem
import com.pixstop.mobile.ui.viewmodel.HomeViewModel

/**
 * Itens do Bottom Navigation
 * Customize esta lista para adicionar/remover abas
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        route = "home",
        label = "Início",
        icon = AppIconType.Home
    ),
    BottomNavItem(
        route = "search",
        label = "Buscar",
        icon = AppIconType.Search,
    ),
    BottomNavItem(
        route = "notifications",
        label = "Alertas",
        icon = AppIconType.Notifications,
        badge = null  // Altere para um número para mostrar badge
    ),
    BottomNavItem(
        route = "profile",
        label = "Perfil",
        icon = AppIconType.Person
    )
)

/**
 * Tela Home com Navigation Drawer (Sidebar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel { HomeViewModel() },
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Navega para login quando faz logout
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                DrawerContent(
                    user = uiState.user,
                    onLogout = {
                        scope.launch {
                            drawerState.close()
                            viewModel.logout()
                        }
                    }
                )
            }
        }
    ) {
        // Estado da aba selecionada no Bottom Navigation
        var selectedTab by remember { mutableStateOf("home") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Home") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            AppIcon(
                                icon = AppIconType.Menu,
                                contentDescription = "Abrir menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        // Indicador de modo offline
                        if (uiState.isOfflineMode) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Offline", modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                )
            },
            bottomBar = {
                AppBottomNavigation(
                    items = bottomNavItems,
                    selectedRoute = selectedTab,
                    onItemSelected = { route ->
                        selectedTab = route
                        // TODO: Navegue para a tela correspondente ou mude o conteúdo
                        // Exemplo: navController.navigate(route)
                    }
                )
            }
        ) { paddingValues ->
            // Conteúdo baseado na aba selecionada
            when (selectedTab) {
                "home" -> HomeContent(
                    uiState = uiState,
                    onRefresh = viewModel::refreshProfile,
                    modifier = Modifier.padding(paddingValues)
                )
                "search" -> PlaceholderContent(
                    title = "Buscar",
                    icon = AppIconType.Search,
                    modifier = Modifier.padding(paddingValues)
                )
                "notifications" -> PlaceholderContent(
                    title = "Notificações",
                    icon = AppIconType.Notifications,
                    modifier = Modifier.padding(paddingValues)
                )
                "profile" -> PlaceholderContent(
                    title = "Perfil",
                    icon = AppIconType.Person,
                    modifier = Modifier.padding(paddingValues)
                )
                else -> HomeContent(
                    uiState = uiState,
                    onRefresh = viewModel::refreshProfile,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Conteúdo do Drawer (Sidebar) - Padrão Material Design
 */
@Composable
private fun DrawerContent(
    user: User?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        // Header do Drawer com info do usuário
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                    // Avatar
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (user != null) {
                                Text(
                                    text = user.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                AppIcon(
                                    icon = AppIconType.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nome do usuário
                    Text(
                        text = user?.name ?: "Usuário",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Email
                    if (user?.email != null) {
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Itens do menu
        // Adicione mais itens aqui conforme necessidade:
        // NavigationDrawerItem(
        //     icon = { Icon(Icons.Default.Home, contentDescription = null) },
        //     label = { Text("Início") },
        //     selected = true,
        //     onClick = { },
        //     modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        // )

        Spacer(modifier = Modifier.weight(1f))

        // Divider antes do logout
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Botão de Logout
        NavigationDrawerItem(
            icon = {
                AppIcon(
                    icon = AppIconType.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            label = {
                Text(
                    text = "Sair",
                    color = MaterialTheme.colorScheme.error
                )
            },
            selected = false,
            onClick = onLogout,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Conteúdo principal da Home
 */
@Composable
private fun HomeContent(
    uiState: com.pixstop.mobile.ui.viewmodel.HomeUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }

            uiState.user != null -> {
                val user = uiState.user

                // Avatar
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Olá, ${user.name}!",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Card informativo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 Login realizado com sucesso!",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Use o menu lateral para navegar ou sair do app.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botão de refresh
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Atualizar Dados")
                }
            }

            uiState.error != null -> {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onRefresh,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Tentar Novamente")
                }
            }
        }
    }
}

/**
 * Conteúdo placeholder para telas não implementadas
 * Substitua por telas reais conforme desenvolve o app
 */
@Composable
private fun PlaceholderContent(
    title: String,
    icon: AppIconType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                AppIcon(
                    icon = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Esta tela ainda não foi implementada.\nSubstitua PlaceholderContent pela sua tela.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
