package com.pixstop.mobile.domain.model

/**
 * O resultado de qualquer chamada: deu certo ou deu errado, sem meio-termo.
 *
 * Chama-se `Outcome` e não `Result` para não colidir com o `kotlin.Result`.
 */
sealed interface Outcome<out T> {

    data class Success<T>(val value: T) : Outcome<T>

    data class Failure(val error: DomainError) : Outcome<Nothing>

    val successOrNull: T? get() = (this as? Success)?.value

    val errorOrNull: DomainError? get() = (this as? Failure)?.error

    val isSuccess: Boolean get() = this is Success

}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) action(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(action: (DomainError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) action(error)
    return this
}
