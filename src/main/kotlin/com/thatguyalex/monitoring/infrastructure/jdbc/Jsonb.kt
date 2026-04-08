package com.thatguyalex.monitoring.infrastructure.jdbc

import org.postgresql.util.PGobject
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

/**
 * Wrapper type for JSONB columns. Spring Data JDBC cannot map plain String
 * to PostgreSQL JSONB without a custom converter, so this wrapper + converter
 * pair lets CrudRepository.save() work transparently.
 */
data class Jsonb(val value: String) {
    override fun toString(): String = value
}

@WritingConverter
class JsonbWritingConverter : Converter<Jsonb, PGobject> {
    override fun convert(source: Jsonb): PGobject {
        val obj = PGobject()
        obj.type = "jsonb"
        obj.value = source.value
        return obj
    }
}

@ReadingConverter
class JsonbReadingConverter : Converter<PGobject, Jsonb> {
    override fun convert(source: PGobject): Jsonb {
        return Jsonb(source.value ?: "{}")
    }
}
