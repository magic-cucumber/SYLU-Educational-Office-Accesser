package top.kagg886.sylu_eoa.api.graduate.util

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

internal object KsoupDocumentSerializer : KSerializer<Document> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.fleeksoft.ksoup.nodes.Document", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Document) {
        encoder.encodeString(value.outerHtml())
    }

    override fun deserialize(decoder: Decoder): Document {
        return Ksoup.parse(decoder.decodeString())
    }
}

internal val HtmlFormat = object : StringFormat {
    override val serializersModule: SerializersModule = SerializersModule {
        contextual(Document::class, KsoupDocumentSerializer)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        check(value is Document) {
            "Only Document can be serialized to HTML"
        }
        return value.outerHtml()
    }

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        check(deserializer.descriptor.serialName == KsoupDocumentSerializer.descriptor.serialName) {
            "Only Document can be deserialized from HTML"
        }
        return Ksoup.parse(string) as T
    }
}
