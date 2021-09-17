package com.zbapp.crpyto

import com.virgilsecurity.android.common.model.FindUsersResult
import com.virgilsecurity.android.common.model.Group
import com.virgilsecurity.sdk.cards.Card
import java.util.*

data class CryptoGroupData(
    private val group: Group
) {

    fun encryptData(data: String): String {
        return group.encrypt(data)
    }

    fun decryptData(senderCard: Card, data: String, dataDate: Date? = null): String {
        return group.decrypt(data, senderCard, dataDate)
    }

    suspend fun addUsers(users: FindUsersResult) {
        blocking {
            group.add(users).execute()
        }
    }

    suspend fun removeUsers(users: FindUsersResult) {
        blocking {
            group.remove(users).execute()
        }
    }

    fun getParticipantsIds(): List<String> {
        return group.participants.distinct()
    }

    val ownerId: String get() = group.initiator

}
