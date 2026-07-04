package com.ilyne.helloszigetkmp.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(tags: List<String>): String =
        Json.encodeToString(ListSerializer(String.serializer()), tags)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        Json.decodeFromString(ListSerializer(String.serializer()), value)
}
