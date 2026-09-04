package com.pixstop.mobile.core.di

import com.pixstop.mobile.core.network.HttpClientFactory
import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.core.storage.TokenManager
import com.pixstop.mobile.data.repository.AccountRepository
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.ui.viewmodel.HomeViewModel
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
}

/** ViewModels, criados a cada tela. */
val viewModelModule: Module = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { SessionViewModel(get(), get()) }
}

/** Tudo que o `startKoin` precisa carregar. */
val appModules: List<Module> = listOf(coreModule, dataModule, viewModelModule)
