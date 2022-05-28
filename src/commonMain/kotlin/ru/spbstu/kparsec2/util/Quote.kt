package ru.spbstu.kparsec2.util

val escapes = mapOf(
    '\r' to "\\r",
    '\n' to "\\n",
    '\t' to "\\t",
    '\b' to "\\b",
    '"' to "\\\"",
    '\'' to "\\\'",
    Char(0) to "\\0"
)

fun quoteString(str: CharSequence): String {
    val sb = StringBuilder("\"")
    for (ch in str) {
        if (ch in escapes) sb.append(escapes[ch]!!)
        else sb.append(ch)
    }
    sb.append("\"")
    return "$sb"
}

fun quoteChar(char: Char): String {
    val sb = StringBuilder("\'")
    if (char in escapes) sb.append(escapes[char]!!)
    else sb.append(char)
    sb.append("\'")
    return "$sb"
}
