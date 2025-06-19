package top.kagg886.sylu_eoa.api.v2.bean

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class SystemNotice(
    @SerialName("cjsj")
    @Serializable(with = NotStandardISOLocalDateTimeSerializer::class)
    val createTime: LocalDateTime,
    @SerialName("xxbt")
    val title: String,
    @SerialName("xxnr")
    val content: String,

    @SerialName("zjxx")
    val id: String,
)


private object NotStandardISOLocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    private val format = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        dayOfMonth()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(format.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val str = decoder.decodeString()
        return LocalDateTime.parse(str, format)
    }
}