package app.paralino.core.utils.extensions

import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

fun Any?.toJsonElement(): JsonElement = when (this) {
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> this.toJsonArray()
    is List<*> -> this.toJsonArray()
    is Map<*, *> -> this.toJsonObject()
    is JsonElement -> this
    else -> JsonNull
}

fun Array<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })
fun Iterable<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })
fun Map<*, *>.toJsonObject() =
    JsonObject(mapKeys { it.key.toString() }.mapValues { it.value.toJsonElement() })

fun JsonElement.toMapOfPrimitives(): Any? {
    val value =
        this as? JsonObject? ?: this as? JsonArray ?: this as? JsonPrimitive ?: this as? JsonNull

    val result = when (value) {
        is JsonArray -> {
            value.map { it.toMapOfPrimitives() }
        }
        is JsonObject -> {
            value.mapValues {
                it.value.toMapOfPrimitives()
            }
        }
        is JsonPrimitive -> {
            if (value.isString) value.content
            else value.booleanOrNull ?: value.doubleOrNull ?: value.content as? Any
        }
        is JsonNull -> null
        else -> throw SerializationException("Unable to parse object - not a json")
    }

    return result
}

fun String.fromJsonObjectToMapOfPrimitives(): Map<String, Any?> {
    val json = BaseRemoteModule.formatter.parseToJsonElement(this).jsonObject
    return json.toMapOfPrimitives() as Map<String, Any?>
}
