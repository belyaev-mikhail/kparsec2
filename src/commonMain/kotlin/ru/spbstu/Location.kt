package ru.spbstu

import kotlin.jvm.JvmInline

fun interface Location<in T> {
    operator fun invoke(token: T): Location<T>

    val canSkip: Boolean get() = false
    fun skip(tokens: Int): Location<T> = error("${this::class} cannot skip characters")
}

object NoLocation: Location<Any?> {
    override fun invoke(token: Any?): NoLocation = this
    override fun toString(): String = "<unknown>"
    override val canSkip: Boolean get() = true

    override fun skip(tokens: Int): NoLocation = this
}

data class CharLocation(val line: Int = 1, val col: Int = 0): Location<Char> {
    override fun invoke(token: Char): CharLocation = when(token) {
        '\n' -> copy(line = line + 1, col = 0)
        else -> copy(col = col + 1)
    }

    override fun toString(): String = "$line:$col"
}

@JvmInline
value class OffsetLocation<T>(val offset: Int): Location<T> {
    override fun invoke(token: T): OffsetLocation<T> = OffsetLocation(offset + 1)
    override fun toString(): String = "$offset"

    override val canSkip: Boolean
        get() = true

    override fun skip(tokens: Int): OffsetLocation<T> = OffsetLocation(offset + tokens)
}
