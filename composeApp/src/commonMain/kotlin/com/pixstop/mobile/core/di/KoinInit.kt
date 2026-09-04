package com.pixstop.mobile.core.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Sobe o Koin uma vez por processo.
 *
 * Cada plataforma chama isto no seu ponto de entrada — o Android no
 * `Application`, o iOS no início do app — passando o que só ela sabe.
 */
fun initKoin(declaration: KoinAppDeclaration? = null) {
    startKoin {
        declaration?.invoke(this)
        modules(appModules)
    }
}
