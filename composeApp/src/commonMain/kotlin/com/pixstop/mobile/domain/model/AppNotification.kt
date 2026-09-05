package com.pixstop.mobile.domain.model

/**
 * Um aviso na caixa do sino.
 */
data class AppNotification(
    val id: String,
    val type: String?,
    val title: String,
    val body: String?,
    val actionUrl: String?,
    val read: Boolean,
    val createdAt: String?,
)
