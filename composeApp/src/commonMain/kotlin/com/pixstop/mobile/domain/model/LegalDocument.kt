package com.pixstop.mobile.domain.model

/**
 * Termo ou política que a pessoa precisa aceitar para usar o app.
 */
data class LegalDocument(
    val id: Long,
    val type: String,
    val typeLabel: String,
    val title: String,
    val version: String,
    val content: String,
    val accepted: Boolean,
)
