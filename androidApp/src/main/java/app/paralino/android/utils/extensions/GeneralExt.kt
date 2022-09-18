package app.paralino.android.utils.extensions

@Suppress("UNCHECKED_CAST")
fun <T> Any?.safeCast(): T? {
    return this as? T?
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.cast(): T {
    return this as T
}