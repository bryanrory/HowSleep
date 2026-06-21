package com.howsleep.app.data.db.converter

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * TypeConverter entre Long (epoch day) e LocalDate.
 * As entities armazenam epoch days como Long por performance;
 * este converter habilita uso direto de LocalDate em queries quando necessário.
 */
class DateConverters {

    @TypeConverter
    fun fromEpochDay(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
