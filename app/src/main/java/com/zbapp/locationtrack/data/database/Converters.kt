package com.zbapp.locationtrack.data.database

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LocalDateTimeConverter {
    @TypeConverter
    fun toDate(dateString: String?): LocalDateTime? {
        return try {
            dateString?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toDateString(date: LocalDateTime?): String? {
        return date?.toString()
    }
}

class LocalDateConverter {
    @TypeConverter
    fun toDate(dateString: String?): LocalDate? {
        return try {
            dateString?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toDateString(date: LocalDate?): String? {
        return date?.toString()
    }
}

class LocalTimeConverter {
    @TypeConverter
    fun toDate(timeString: String?): LocalTime? {
        return try {
            timeString?.let { LocalTime.parse(it) }
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toDateString(time: LocalTime?): String? {
        return time?.toString()
    }
}

class ListOfStringsConverters {

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return Json.decodeFromString<List<String>>(value ?: return null)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return Json.encodeToString(list ?: return null)
    }

}