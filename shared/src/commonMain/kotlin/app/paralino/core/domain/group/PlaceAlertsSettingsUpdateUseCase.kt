package app.paralino.core.domain.group

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.models.places.GroupPlaceSettingsData
import app.paralino.core.models.places.GroupPlaceSettingsEntry
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.PlacesRemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PlaceAlertsSettingsUpdateUseCase(
    private val remote: PlacesRemoteSource,
    private val local: PlacesLocalDataSource,
) : LimitedExecutionRepository() {

    fun updatePlaceAlertsSettings(
        placeId: String,
        active: Boolean,
        settings: List<GroupPlaceSettingsEntry>?
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val place = local.getPlaceWithSettings(placeId = placeId).first()
                val currentSettings = place.settings?.alertData

                val settings = if (currentSettings != null) {
                    currentSettings.copy(
                        isActive = active,
                        settings = settings ?: currentSettings.settings
                    )
                } else {
                    GroupPlaceSettingsData(
                        isActive = active,
                        settings = emptyList()
                    )
                }
                remote.updatePlaceSettings(
                    groupId = place.place.groupId,
                    placeId = place.place.id,
                    placeSettingsId = place.settings?.id,
                    alertData = settings
                )
            },
            localUpdate = {
                local.updatePlaceSettings(it)
            }
        )
            .startProcessing("place_settings")
            .map { Unit }
    }

    fun updatePlaceAlertsSettings(
        placeId: String,
        data: GroupPlaceSettingsEntry
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val place = local.getPlaceWithSettings(placeId = placeId).first()
                val currentSettings = place.settings?.alertData

                val settings = if (currentSettings != null) {
                    val currentAlert =
                        currentSettings.settings.firstOrNull { it.memberUserId == data.memberUserId }

                    val newAlerts = if (currentAlert != null) {
                        currentSettings.settings.toMutableList().apply {
                            removeAll { it.memberUserId == data.memberUserId }
                            add(data)
                        }
                    } else {
                        currentSettings.settings.toMutableList().apply { add(data) }
                    }

                    currentSettings.copy(settings = newAlerts)
                } else {
                    GroupPlaceSettingsData(
                        isActive = true,
                        settings = listOf(data)
                    )
                }
                remote.updatePlaceSettings(
                    groupId = place.place.groupId,
                    placeId = place.place.id,
                    placeSettingsId = place.settings?.id,
                    alertData = settings
                )
            },
            localUpdate = {
                local.updatePlaceSettings(it)
            }
        )
            .startProcessing("place_settings")
            .map { Unit }
    }
}