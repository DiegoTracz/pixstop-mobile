package com.pixstop.mobile.core.di

import com.pixstop.mobile.core.network.HttpClientFactory
import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.core.storage.TokenManager
import com.pixstop.mobile.data.repository.AccountRepository
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.data.repository.LegalRepository
import com.pixstop.mobile.data.repository.NotificationRepository
import com.pixstop.mobile.data.repository.ProfileRepository
import com.pixstop.mobile.ui.viewmodel.LegalConsentViewModel
import com.pixstop.mobile.ui.viewmodel.NotificationsViewModel
import com.pixstop.mobile.ui.viewmodel.ProfileViewModel
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
}

/** ViewModels, criados a cada tela. */
val viewModelModule: Module = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { SessionViewModel(get(), get(), get()) }
    viewModel { LegalConsentViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { NotificationsViewModel(get()) }
}

/** Tudo que o `startKoin` precisa carregar. */
val appModules: List<Module> = listOf(coreModule, dataModule, viewModelModule)
