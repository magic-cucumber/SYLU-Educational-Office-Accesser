package top.kagg886.eoa.gym.internal

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant


internal object NotStandardISOInstantSerializer : KSerializer<Instant> {
    private val DEFAULT_TIMEZONE = TimeZone.of("Asia/Shanghai")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    private val format = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toLocalDateTime(DEFAULT_TIMEZONE).format(format))
    }

    override fun deserialize(decoder: Decoder): Instant {
        val str = decoder.decodeString()
        return LocalDateTime.parse(str, format).toInstant(DEFAULT_TIMEZONE)
    }
}
