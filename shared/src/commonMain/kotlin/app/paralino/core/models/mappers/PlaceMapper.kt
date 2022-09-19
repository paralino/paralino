package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class PlaceMapper constructor(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore
) {

    suspend fun core2Remote(model: GroupPlace): GroupPlaceRaw {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.id,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion
        ) ?: throw MissingGroupKeyException(model.id)

        val placeData = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(model.data),
            key = groupKey.data.content
        )

        return GroupPlaceRaw(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion,
            private = model.private,
            data = placeData
        )
    }

    suspend fun remote2Core(model: GroupPlaceRaw): GroupPlace {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.id,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion
        ) ?: throw MissingGroupKeyException(model.id)

        val placeData = cryptoCore.decryptDataWithKey(
            data = model.data,
            key = groupKey.data.content
        ).let {
            BaseRemoteModule.formatter.decodeFromString<GroupPlaceData>(it)
        }

        return GroupPlace(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion,
            private = model.private,
            data = placeData,
        )
    }

}