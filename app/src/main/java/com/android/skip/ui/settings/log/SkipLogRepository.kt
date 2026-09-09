package com.android.skip.ui.settings.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.skip.MyApp
import com.android.skip.R
import com.android.skip.util.DataStoreUtils
import com.blankj.utilcode.util.StringUtils.getString
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class SkipLogEntry(
    val timestamp: Long,
    val level: String,
    val source: String,
    val message: String,
    val stackTrace: String?
)

data class SkipLogDay(
    val date: String,
    val entries: List<SkipLogEntry>
)

@Singleton
class SkipLogRepository @Inject constructor() {
    private val gson = Gson()
    private val dayFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val storeKey = getString(R.string.store_skip_log)
    private val logDir: File
        get() = File(MyApp.context.filesDir, "skip-logs")

    private val _enable = MutableLiveData(
        DataStoreUtils.getSyncData(storeKey, false)
    )

    val enable: LiveData<Boolean> = _enable

    fun changeEnable(enable: Boolean) {
        _enable.postValue(enable)
    }

    suspend fun appendExceptionLog(
        level: String,
        source: String,
        message: String,
        throwable: Throwable? = null
    ) {
        if (!DataStoreUtils.getSyncData(storeKey, false)) {
            return
        }

        val now = Instant.now()
        val fileName = "${dayFormatter.format(now.atZone(ZoneId.systemDefault()).toLocalDate())}.jsonl"
        val targetFile = File(logDir, fileName)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val entry = SkipLogEntry(
            timestamp = now.toEpochMilli(),
            level = level,
            source = source,
            message = message,
            stackTrace = throwable?.toStackTraceString()
        )
        targetFile.appendText("${gson.toJson(entry)}\n")
    }

    fun readDailyLogs(): List<SkipLogDay> {
        if (!logDir.exists()) {
            return emptyList()
        }

        return logDir.listFiles { file -> file.isFile && file.extension == "jsonl" }
            ?.sortedByDescending { file ->
                runCatching {
                    LocalDate.parse(file.nameWithoutExtension, dayFormatter)
                }.getOrDefault(LocalDate.MIN)
            }
            ?.mapNotNull { file ->
                val entries = file.readLines()
                    .asSequence()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line -> parseEntry(line) }
                    .sortedByDescending { it.timestamp }
                    .toList()

                if (entries.isEmpty()) {
                    null
                } else {
                    SkipLogDay(file.nameWithoutExtension, entries)
                }
            }
            ?: emptyList()
    }

    fun clearAllLogs() {
        if (!logDir.exists()) {
            return
        }
        logDir.listFiles()?.forEach {
            if (it.isFile) {
                it.delete()
            }
        }
    }

    private fun parseEntry(line: String): SkipLogEntry? {
        val element = runCatching { JsonParser.parseString(line) }.getOrNull() ?: return null
        if (!element.isJsonObject) {
            return null
        }
        val obj = element.asJsonObject
        val timestamp = obj.get("timestamp")?.asLong ?: return null

        // Backward compatibility: convert old click-log format into readable message lines.
        val hasExceptionFields = obj.has("level") || obj.has("source") || obj.has("message")
        return if (hasExceptionFields) {
            SkipLogEntry(
                timestamp = timestamp,
                level = obj.get("level")?.asString ?: "E",
                source = obj.get("source")?.asString ?: "Unknown",
                message = obj.get("message")?.asString ?: "",
                stackTrace = obj.get("stackTrace")?.asString
            )
        } else {
            val packageName = obj.get("packageName")?.asString ?: "unknown"
            val activityName = obj.get("activityName")?.asString ?: "unknown"
            val rect = obj.get("rect")?.asString ?: "unknown"
            SkipLogEntry(
                timestamp = timestamp,
                level = "I",
                source = "Legacy",
                message = "点击日志: package=$packageName activity=$activityName rect=$rect",
                stackTrace = null
            )
        }
    }

    private fun Throwable.toStackTraceString(): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        this.printStackTrace(pw)
        return sw.toString()
    }
}