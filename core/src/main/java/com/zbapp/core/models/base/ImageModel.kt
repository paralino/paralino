package com.zbapp.core.models.base

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ImageModelStringSerializer::class)
data class ImageModel(val image: String = "") {

    fun getImageType(): ImageType? {
        return ImageType.values().firstOrNull { image.startsWith("${it.name}:") }
    }

    fun getImageValue(): String? {
        return getImageType()?.let { image.replaceFirst("${it.name}:", "") }
    }

    companion object {
        fun emoji(emoji: String): ImageModel {
            return ImageModel(ImageType.emoji.createModel(emoji))
        }

        fun raw(raw: String): ImageModel {
            return ImageModel(ImageType.raw.createModel(raw))
        }
    }
}

fun String.asImageModel(): ImageModel? {
    return ImageModel(this).takeIf { it.getImageType() != null }
}

enum class ImageType {
    emoji, raw;

    fun createModel(value: String): String {
        return "$name:$value"
    }
}

object ImageModelStringSerializer : KSerializer<ImageModel> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ImageModel", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ImageModel) {
        encoder.encodeString(value.image)
    }

    override fun deserialize(decoder: Decoder): ImageModel {
        val string = decoder.decodeString()
        return ImageModel(string)
    }
}