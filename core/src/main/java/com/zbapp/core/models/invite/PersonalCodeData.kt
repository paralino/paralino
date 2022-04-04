package com.zbapp.core.models.invite

import com.zbapp.core.models.user.ImageModel
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalCodeData constructor(

    @SerialName("\$id")
    val id: String = "",

    val ownerId: String,
    val ownerName: String,
    val ownerImage: ImageModel? = null,
    val ownerKey: String,
    val validity: Long,
    val code: String,
    val urlCode: String
) {

    fun isStillValid(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return validity <= 0 || validity >= now
    }

}

