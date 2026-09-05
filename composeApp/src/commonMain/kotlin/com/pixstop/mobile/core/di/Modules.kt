package com.pixstop.mobile.core.di

import com.pixstop.mobile.core.network.HttpClientFactory
import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.core.storage.TokenManager
import com.pixstop.mobile.data.repository.AccountRepository
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.data.repository.LegalRepository
import com.pixstop.mobile.data.repository.CartRepository
import com.pixstop.mobile.data.repository.NotificationRepository
import com.pixstop.mobile.data.repository.OrderRepository
import com.pixstop.mobile.data.repository.ProfileRepository
import com.pixstop.mobile.data.repository.ShopRepository
import com.pixstop.mobile.data.repository.TeamRepository
import com.pixstop.mobile.ui.viewmodel.LegalConsentViewModel
import com.pixstop.mobile.ui.viewmodel.CartViewModel
import com.pixstop.mobile.ui.viewmodel.CheckoutViewModel
import com.pixstop.mobile.ui.viewmodel.NotificationsViewModel
import com.pixstop.mobile.ui.viewmodel.OrderViewModel
import com.pixstop.mobile.ui.viewmodel.OrdersViewModel
import com.pixstop.mobile.ui.viewmodel.PixelHistoryViewModel
import com.pixstop.mobile.ui.viewmodel.TeamViewModel
import com.pixstop.mobile.ui.viewmodel.ProductDetailViewModel
import com.pixstop.mobile.ui.viewmodel.ProfileViewModel
import com.pixstop.mobile.ui.viewmodel.ShopViewModel
import com.pixstop.mobile.ui.viewmodel.LoginViewModel
import com.pixstop.mobile.ui.viewmodel.RegisterViewModel
import com.pixstop.mobile.ui.viewmodel.SessionViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Infraestrutura: armazenamento, sessão e cliente HTTP.
 *
 * Tudo aqui é `single` porque é caro de criar e precisa ser o mesmo em todo o
 * app — em especial o `HttpClient`, que antes era recriado a cada login.
 */
val coreModule: Module = module {
    single { TokenManager() }
    single { SessionStore(get()) }
    single<HttpClient> { HttpClientFactory.create(get()) }
}

/** Repositórios: a fronteira entre a API e o resto do app. */
val dataModule: Module = module {
    single { AuthRepository(get(), get(), get()) }
    single { AccountRepository(get()) }
    single { LegalRepository(get()) }
    single { ProfileRepository(get()) }
    single { NotificationRepository(get()) }
    single { ShopRepository(get()) }
    single { CartRepository(get()) }
    single { OrderRepository(get()) }
    single { TeamRepository(get()) }
}

/** ViewModels, criados a cada tela. */
val viewModelModule: Module = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { SessionViewModel(get(), get(), get()) }
    viewModel { LegalConsentViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { NotificationsViewModel(get(), get()) }
    viewModel { ShopViewModel(get()) }
    viewModel { ProductDetailViewModel(get()) }
    viewModel { CartViewModel(get(), get()) }
    viewModel { CheckoutViewModel(get()) }
    viewModel { OrderViewModel(get()) }
    viewModel { OrdersViewModel(get()) }
    viewModel { PixelHistoryViewModel(get(), get()) }
    viewModel { TeamViewModel(get()) }
}

/** Tudo que o `startKoin` precisa carregar. */
val appModules: List<Module> = listOf(coreModule, dataModule, viewModelModule)
