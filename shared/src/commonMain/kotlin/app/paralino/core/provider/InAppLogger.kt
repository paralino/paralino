package app.paralino.core.provider

import app.paralino.core.data.db.DebugLogsEntity
import app.paralino.core.data.db.LogsDatabaseHelper
import app.paralino.core.data.db.execute
import app.paralino.core.data.db.mapToList
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class InAppLogger(
    private val database: LogsDatabaseHelper,
    private val scopeProvider: ICoroutineScopeProvider,
    private val appBuildConfig: AppBuildConfig
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
                        scopeProvider.userSessionIOScope.launch {
                            try {
                                database.transaction {
                                    logs.forEach {
                                        database.debugLogsQueries.insertEntry(
                                            timeUTC = it.timeUTC,
                                            type = it.type,
                                            priority = it.priority,
                                            tag = it.tag,
                                            title = it.title,
                                            info = it.info,
                                            host = it.host
                                        )
                                        database.debugLogsQueries.removeOlderThan(
                                            nowUTC().minus(2.days).toString()
                                        )
                                    }
                                }
                                _pendingLogsFlow.update { emptyList() }
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

    override fun systemLog(priority: LogPriority, tag: String?, message: String, t: Throwable?) {

        if (!appBuildConfig.isDevMode) {
            return
        }

        if (tag == "app_logger") {
            return
        }

        if (priority < LogPriority.INFO) {
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
            host = appBuildConfig.appHost.key,
            priority = priority.key,
            tag = tag ?: "default",
            title = title,
            info = info,
        )

        _pendingLogsFlow.update { it + entry }
    }

    fun getFilteredLog(
        priorities: List<LogPriority>,
        types: List<InAppLoggerLogType>,
        tags: List<String>,
        hosts: List<AppHost>,
        text: String?
    ): Flow<List<DebugLogsEntity>> {
        return database.mapToList {
            debugLogsQueries.getFiltered(
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
    }

    suspend fun clearLogs() {
        _pendingLogsFlow.update { listOf() }
        database.execute { debugLogsQueries.clearAll() }
    }
}

fun DebugLogsEntity.print(): String {
    return "${
        this.timeUTC.isoWithOffsetToInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
            ?.format(format = LocalDateTime.Formats.ISO)?.replace("T", " ")
    } [${this.type}] [${LogPriority.fromKeyOrFloor(this.priority)}]\n[${this.tag}]\n${this.title}${this.info?.let { " | ${this.info}" } ?: ""}\n"
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