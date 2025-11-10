package app.paralino.core.repo.user

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.location.LocationHistoryModel
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

interface IExportPersonalLocationHistoryUseCase {
    suspend fun exportPersonalLocationHistoryUntil(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Boolean>
}

interface IImportPersonalLocationHistoryUseCase {
    suspend fun importPersonalLocationHistoryFromFile(
        fileName: String,
        locationId: String
    ): Result<Unit>
}

@Serializable
data class ExportableLocationHistoryModelForUser(
    val groupId: String,
    val memberUserId: String,
    val history: List<ExportableLocationHistoryModelForDevice>
)

@Serializable
data class ExportableLocationHistoryModelForDevice(
    val locationId: String?,
    val entryId: String,
    val dateKey: String,
    val historyKeyVersion: String,
    val isCachedLocation: Boolean,
    val history: LocationHistoryModel
)