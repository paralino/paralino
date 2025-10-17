package app.paralino.core.domain.location.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.keys.PersonalGroupKeyEntity
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.mappers.LocationHistoryMapper
import app.paralino.core.models.raw.GroupLocationHistoryReadPayload
import app.paralino.core.models.raw.GroupLocationHistoryReadResponse
import app.paralino.core.models.raw.PersonalLocationHistoryReadPayload
import app.paralino.core.models.raw.PersonalLocationHistoryReadResponse
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.utils.extensions.historyDateKeyForApi
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.listOfDatesUntil
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class RefreshLocationHistoryUseCase(
    private val database: MainDatabaseHelper,
    private val mapper: LocationHistoryMapper,
    private val functionsApi: IFunctionsApi,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val userGroupRepo: UserGroupLocalDataSource,
    private val remoteConfig: RemoteConfigRepository,
    private val appPrefs: AppPrefs,
) : BaseRepository() {

    private val mutex = Mutex()
    private val keyCache = MutableStateFlow<Map<String, HistoryFetchCacheKey>>(mapOf())

    suspend fun refreshLocationHistory(
        groupId: GroupOrSpaceId,
        memberUserId: String,
        locationId: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        requestCacheDuration: Duration
    ) {
        mutex.withLock {
            val dates = startDate.listOfDatesUntil(endDate)
            if (groupId.isPersonalSpace) {
                refreshPersonalLocationHistory(
                    locationId = locationId,
                    dates = dates,
                    requestCacheDuration = requestCacheDuration
                )
            } else {
                refreshGroupLocationHistory(
                    groupId = groupId.id,
                    memberUserId = memberUserId,
                    locationId = locationId,
                    dates = dates,
                    requestCacheDuration = requestCacheDuration
                )
            }
        }
    }

    private suspend fun refreshGroupLocationHistory(
        groupId: String,
        memberUserId: String,
        locationId: String?,
        dates: List<LocalDate>,
        requestCacheDuration: Duration
    ): GroupLocationHistoryReadResponse? {

        val group = userGroupRepo.allGroupsState.value.data?.firstOrNull { it.group.id == groupId }
        if (group?.group?.planConfig(remoteConfig.getConfigOnce())?.isGroupHistoryEnabled != true) {
            logg.d("history_refresh", "Skip history upload for group $groupId - history disabled")
            return null
        }

        val keyPrefix = "${groupId}_${memberUserId}_"
        val datesToLoad = dates.filter { date ->
            val key = "${keyPrefix}${date.historyDateKeyForApi()}"
            val point = keyCache.value[key]
            point?.time?.isOlderThan(requestCacheDuration) ?: true
        }

        if (datesToLoad.isEmpty()) {
            return null
        }

        val dateKeys = datesToLoad.map { it.historyDateKeyForApi() }
        val key = dateKeys.joinToString(separator = "_")
        return triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "${groupId}_${memberUserId}_${key}",
                timeMillis = requestCacheDuration.inWholeMilliseconds
            )
        ) {
            val payload = GroupLocationHistoryReadPayload(
                groupId = groupId,
                memberUserId = memberUserId,
                locationId = locationId,
                dateKeys = dateKeys
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.HISTORY_READ_GROUP,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            return@triggerRemoteMaybe response.response<GroupLocationHistoryReadResponse>().data
        }?.also { response ->
            updateCache(
                keyPrefix = keyPrefix,
                dates = datesToLoad
            )

            val historyKeys = response.historyKeys.map {
                mapper.remote2LocalGroup(groupId = groupId, memberUserId = memberUserId, model = it)
            }

            val kxKeys = response.kxKeys.map {
                PersonalGroupKeyEntity(
                    id = it.keyId,
                    ownerUserId = appPrefs.userId,
                    groupId = groupId,
                    encryptedUserKeys = it.encryptedKeys,
                    personalGroupKeyVersion = it.memberKeyVersion,
                    userKeyVersion = it.userKeyVersion,
                    isArchived = true
                )
            }

            val entries = response.entries.map {
                mapper.remote2LocalGroup(groupId = groupId, memberUserId = memberUserId, model = it)
            }

            database.locationHistoryDao.recreateGroupHistory(
                dates = response.dateKeys,
                groupId = groupId,
                memberUserId = memberUserId,
                entries = entries,
                historyKeys = historyKeys,
                kxKeys = kxKeys
            )
        }
    }

    private suspend fun refreshPersonalLocationHistory(
        locationId: String?,
        dates: List<LocalDate>,
        requestCacheDuration: Duration
    ): PersonalLocationHistoryReadResponse? {

        if (personalSpaceRepo.personalSpaceState.value.data?.plan?.isPersonalHistoryEnabled != true) {
            logg.d("history_refresh", "Skip history upload for personal space - history disabled")
            return null
        }

        val keyPrefix = "personal_history_"
        val datesToLoad = dates.filter { date ->
            val key = "${keyPrefix}${date.historyDateKeyForApi()}"
            val point = keyCache.value[key]
            point?.time?.isOlderThan(requestCacheDuration) ?: true
        }

        if (datesToLoad.isEmpty()) {
            return null
        }

        val dateKeys = datesToLoad.map { it.historyDateKeyForApi() }
        val key = dateKeys.joinToString(separator = "_")
        return triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "personal_history_${key}",
                timeMillis = requestCacheDuration.inWholeMilliseconds
            )
        ) {
            val payload = PersonalLocationHistoryReadPayload(
                locationId = locationId,
                dateKeys = dateKeys
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.HISTORY_READ_PERSONAL,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            return@triggerRemoteMaybe response.response<PersonalLocationHistoryReadResponse>().data
        }?.also { response ->
            updateCache(
                keyPrefix = keyPrefix,
                dates = datesToLoad
            )

            val keys = response.keys.map {
                mapper.remote2LocalPersonal(model = it)
            }

            val entries = response.entries.map {
                mapper.remote2LocalPersonal(model = it)
            }

            database.locationHistoryDao.recreatePersonalHistory(
                dates = response.dateKeys,
                entries = entries,
                historyKeys = keys
            )
        }
    }

    private fun updateCache(
        keyPrefix: String,
        dates: List<LocalDate>
    ) {
        keyCache.update {
            it
                .filter { !it.value.time.isOlderThan(1.hours) }
                .toMutableMap().apply {
                    val time = nowUTC()
                    dates.forEach {
                        val key = "${keyPrefix}${it.historyDateKeyForApi()}"
                        put(
                            key = key,
                            value = HistoryFetchCacheKey(
                                key = key,
                                time = time
                            )
                        )
                    }
                }
        }
    }

    private data class HistoryFetchCacheKey(
        val time: Instant,
        val key: String
    )
}