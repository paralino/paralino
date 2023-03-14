package app.paralino.android.utils.extensions

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Any?.safeCast(): T? {
    try {
        return this as? T?
    } catch (e: Exception) {
        return null
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.cast(): T {
    return this as T
}