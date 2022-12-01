package app.paralino.core.domain.location

import app.paralino.core.config
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.places.IPlacesLocalDataSource
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class UserMemberIdUseCase constructor(
    private val userRepository: UserRepository,
    private val memberLocal: ILocationLocalDataSource,
    private val placeLocal: IPlacesLocalDataSource
) {

    suspend fun getCurrentDeviceType(): String {
        // TODO add proper device type
        return "android"
    }

    suspend fun getCurrentSessionId(): String {
        return config.sessionId
    }

    suspend fun getCurrentLocationForGroup(groupId: String): MemberLocation? {
        return memberLocal.getMember(
            groupId = groupId,
            userId = userRepository.cachedUserId
        ).first().locations.firstOrNull {
            it.deviceId == config.sessionId
        }
    }

    suspend fun getCurrentLocationIdForGroup(groupId: String): String? {
        return getCurrentLocationForGroup(groupId)?.id
    }

    suspend fun getCurrentUserMemberIdForGroup(groupId: String): String? {
        return memberLocal.getUserMemberId(
            groupId = groupId,
            userId = userRepository.cachedUserId
        )
    }

    suspend fun getCurrentUserMemberForGroup(groupId: String): MemberModelWithLocations? {
        return memberLocal.getMember(
            groupId = groupId,
            userId = userRepository.cachedUserId
        ).firstOrNull()
    }

    suspend fun getCurrentUserMemberIdForPlace(placeId: String): String? {
        val place = placeLocal.getPlaceWithoutSettingsOnce(placeId)
        return place?.groupId?.let {
            getCurrentUserMemberIdForGroup(it)
        }
    }

}