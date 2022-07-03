package app.paralino.core.utils.extensions

fun randomString(length: Int = 32): String {
    val source = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { source.random() }
        .joinToString("")
}

fun String.splitRanges(wrappedChars: String): List<Pair<String, IntRange>> {
    val ranges = mutableListOf<Pair<String, IntRange>>()
    var isWrapped = false
    var start = 0
    this.split(wrappedChars).forEach {
        if(isWrapped) ranges.add(it to IntRange(start, start + it.length))
        isWrapped = !isWrapped
        start += it.length
    }
    return ranges.filter { pair ->
        val it = pair.second
        it.first >= 0 && it.last <= this.length && it.first < it.last
    }
}