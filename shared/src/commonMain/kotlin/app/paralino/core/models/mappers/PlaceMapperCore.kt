package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException

class PlaceMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    val settingsMapper: PlaceSettingsMapperCore
) {
    suspend fun remote2Core(model: GroupPlaceRaw): GroupPlace {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.groupId,
            keyVersion = model.groupKeyVersion
        )

        if(groupKey !is GroupMasterKey.Valid)
            logg.e(MissingGroupKeyException(model.id, model.groupKeyVersion))

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
            data = placeData,
        )
    }
}