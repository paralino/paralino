package app.paralino.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserSession constructor(
    val id: String,

    val createdAt: String,
    val updatedAt: String,
    val expire: String,
    val provider: String,

    val ip: String,
    val osCode: String,
    val osName: String,
    val osVersion: String,

    val clientType: String,
    val clientCode: String,
    val clientName: String,
    val clientVersion: String,
    val clientEngine: String,
    val clientEngineVersion: String,

    val deviceType: String,
    val deviceName: String,
    val deviceBrand: String,
    val deviceModel: String,

    val countryCode: String,
    val countryName: String,
    val current: Boolean
)
