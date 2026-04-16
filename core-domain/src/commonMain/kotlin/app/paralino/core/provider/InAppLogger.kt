package app.paralino.core.provider

import app.paralino.core.db.LogsDatabaseHelper
import app.paralino.core.db.logs.debug.DebugLogsEntity
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.logg
import app.paralino.core.models.AppHost
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class InAppLogger(
    private val database: LogsDatabaseHelper,
    private val scopeProvider: ICoroutineScopeProvider,
    private val appConfig: AppConfigRepository
) : SystemLogr() {

    private val _pendingLogsFlow = MutableStateFlow<List<DebugLogsEntity>>(emptyList())

    private val maxFlushRate = 2.seconds
    private var lastFlushTime: Instant? = null

    init {
        scopeProvider.defaultIOScope.launch {
            logg.d(tag = "app_logger", message = "Start in-app logger")
            try {
                _pendingLogsFlow
                    .filter { it.isNotEmpty() }
                    .conflate()
                    .collectLatest { logs ->
                        val lastUpdate = lastFlushTime
                        val diff = if (lastUpdate != null) nowUTC() - lastUpdate else 0.seconds
                        if (diff > Duration.ZERO && diff < maxFlushRate) {
                            val delay = maxFlushRate - diff
                            delay(delay)
                        }

                        lastFlushTime = nowUTC()
                        scopeProvider.defaultIOScope.launch {
                            try {
                                _pendingLogsFlow.update { it - logs.toSet() }
                                database.debugLogsQueries.insertNewAndRemoveOldEntries(
                                    maxValidTimeUTC = nowUTC().minus(2.days).toString(),
                                    logs = logs
                                )
                            } catch (e: Exception) {
                                lastFlushTime = null
                                logg.w(
                                    tag = "app_logger",
                                    message = "Error writing new logs: ${e.message}"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                logg.w(
                    tag = "app_logger",
                    message = "Error processing new logs: ${e.message}"
                )
            }
        }
    }

    override fun systemLog(
        priority: LogPriority,
        tag: String?,
        message: String,
        fullOutput: String,
        t: Throwable?
    ) {

        if (!appConfig.isInAppDevMode) {
            return
        }

        if (tag == "app_logger") {
            return
        }

        if (priority < LogPriority.DEBUG && appConfig.appHost == AppHost.MAIN_APP) {
            return
        }

        log(
            type = InAppLoggerLogType.CONSOLE,
            priority = priority,
            tag = tag ?: "default",
            title = message,
            info = t?.message
        )
    }

    fun logNotificationPayload(payload: IPushNotificationPresenter.Payload) {
        if (payload.loggerTag != null) {
            val timeInfo = payload.timeInfo ?: payload.timeMillis?.let { Instant.fromEpochMilliseconds(it).toString() }
            log(
                type = InAppLoggerLogType.NOTIFICATIONS,
                priority = LogPriority.INFO,
                tag = payload.loggerTag,
                title = listOfNotNull(payload.title, payload.message?.let { "($it)" }).joinToString(separator = " "),
                info = timeInfo
            )
        }
    }

    fun log(
        type: InAppLoggerLogType,
        priority: LogPriority,
        tag: String?,
        title: String,
        info: String? = null
    ) {
        val entry = DebugLogsEntity(
            id = 0,
            timeUTC = nowUTC().toString(),
            type = type.key,
            host = appConfig.appHost.key,
            priority = priority.key,
            tag = tag ?: "default",
            title = title,
            info = info,
        )

        if (appConfig.appHost == AppHost.MAIN_APP) {
            _pendingLogsFlow.update { it + entry }
        } else {
            scopeProvider.defaultIOScope.launch {
                try {
                    database.debugLogsQueries.insertNewAndRemoveOldEntries(
                        maxValidTimeUTC = nowUTC().minus(2.days).toString(),
                        logs = listOf(entry)
                    )
                } catch (e: Exception) {
                    logg.w(
                        tag = "app_logger",
                        message = "Error writing new logs: ${e.message}"
                    )
                }
            }
        }
    }

    fun getFilteredLog(
        priorities: List<LogPriority>,
        types: List<InAppLoggerLogType>,
        tags: List<String>,
        hosts: List<AppHost>,
        text: String?
    ): Flow<List<DebugLogsEntity>> {
        return database.debugLogsQueries.getFiltered(
            types = types.map { it.key },
            tags = tags,
            hosts = hosts.map { it.key },
            priorities = priorities.map { it.key },
            typesNullIfEmpty = if (types.isEmpty()) 1 else 0,
            tagsNullIfEmpty = if (tags.isEmpty()) 1 else 0,
            priorityNullIfEmpty = if (priorities.isEmpty()) 1 else 0,
            hostsNullIfEmpty = if (hosts.isEmpty()) 1 else 0,
            text = text ?: ""
        )
    }

    suspend fun clearLogs() {
        _pendingLogsFlow.update { listOf() }
        database.debugLogsQueries.clearAll()
    }
}

fun DebugLogsEntity.print(valueFormatter: IValueFormatter): String {
    return buildString {
        append("[$tag]")
        append("\n")
        append(
            timeUTC.isoWithOffsetToInstant()
                ?.let {
                    valueFormatter.formatWithPattern(
                        timeUTC = it.toLocalDateTime(TimeZone.UTC),
                        pattern = "yyyy-MM-dd' 'HH:mm:ss'.'SSS"
                    )
                }
        )
        append(" [${type}]")
        append(" [${LogPriority.fromKeyOrFloor(priority)}]")
        append(" [$host]")
        append("\n")
        append(title)
        if (!info.isNullOrBlank()) {
            append(" | $info")
        }
        append("\n")
    }
}

enum class InAppLoggerLogType constructor(val key: String) {
    CONSOLE("console_log"),
    NOTIFICATIONS("notifications"),
    GENERIC("generic");

    companion object {
        fun fromKey(key: String): InAppLoggerLogType {
            return entries.firstOrNull { it.key == key } ?: GENERIC
        }
    }
}