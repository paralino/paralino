package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException

class PlaceMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val cryptoCore: CryptoCore,
    val settingsMapper: PlaceSettingsMapperCore
) {
    suspend fun remote2Core(model: GroupPlaceRaw, groupOwnerId: String? = null): GroupPlace {

        val groupKey = if (groupOwnerId.isNullOrBlank()) {
            cryptoKEKRepo.getGroupKey(
                groupId = model.groupId,
                keyVersion = model.groupKeyVersion
            )
        } else {
            cryptoKEKRepo.getGroupKey(
                groupId = model.groupId,
                groupOwnerId = groupOwnerId,
                keyVersion = model.groupKeyVersion
            )
        }

        if(groupKey !is GroupMasterKey.Valid)
            logg.e(MissingGroupKeyException(model.groupId, model.groupKeyVersion))

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
            groupKeyVersion = model.groupKeyVersion,
            private = model.private,
            isInPersonalSpace = model.isInPersonalSpace ?: false,
            data = placeData,
            eventKeys = model.eventKeys
        )
    }
}