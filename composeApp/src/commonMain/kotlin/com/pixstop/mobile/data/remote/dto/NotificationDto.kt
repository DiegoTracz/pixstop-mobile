package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Notificação do sino, como vem do `GET /notifications`.
 *
 * O `type` e o `action_url` existem para o toque levar a algum lugar; até as
 * telas de destino existirem, servem só para escolher o ícone.
 */
@Serializable
data class NotificationDto(
    val id: String,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val icon: String? = null,
    @SerialName("action_url") val actionUrl: String? = null,
    val read: Boolean = false,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
