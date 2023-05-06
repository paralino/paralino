package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class PlaceMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    val settingsMapper: PlaceSettingsMapperCore
) {

    suspend fun core2Remote(model: GroupPlace): GroupPlaceRaw {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.groupId,
            keyVersion = model.keyVersion
        )

        if(groupKey !is GroupMasterKey.Valid)
            throw MissingGroupKeyException(model.groupId, model.keyVersion)

        val placeData = when (model.data) {
            is CryptoResource.Ready -> {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(model.data.data),
                    key = groupKey.key
                )
            }
            else -> throw CryptoException("Place data is missing: ${model.id}")
        }

        return GroupPlaceRaw(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            private = model.private,
            data = placeData
        )
    }

    suspend fun remote2Core(model: GroupPlaceRaw): GroupPlace {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.groupId,
            keyVersion = model.keyVersion
        )

        if(groupKey !is GroupMasterKey.Valid)
            logg.e(MissingGroupKeyException(model.id, model.keyVersion))

        val placeData = if (groupKey is GroupMasterKey.Valid) {
            try {
                cryptoCore.decryptDataWithKey(
                    data = model.data,
                    key = groupKey.key
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<GroupPlaceData>(it)
                }.let {
                    CryptoResource.Ready(it)
                }
            } catch (e: Exception) {
                CryptoResource.WrongKeyOrData
            }
        } else {
            CryptoResource.MissingKey
        }

        return GroupPlace(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            private = model.private,
            data = placeData,
        )
    }

    suspend fun remote2CoreWithSettings(model: GroupPlacesWithSettingsRaw): GroupPlaceWithSettings {
        return GroupPlaceWithSettings(
            place = remote2Core(model.place),
            settings = model.settings?.let { settingsMapper.remote2Core(it) }
        )
    }

}