package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A prévia em pixel art, como data URI. */
@Serializable
data class PixelAvatarPreviewDto(val preview: String)

@Serializable
data class ApplyPixelAvatarDto(@SerialName("avatar_url") val avatarUrl: String)
