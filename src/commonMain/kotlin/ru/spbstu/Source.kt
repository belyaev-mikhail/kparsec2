package ru.spbstu

import ru.spbstu.Source.Companion.dropDefault
import ru.spbstu.parsers.combinators.many
import ru.spbstu.parsers.success
import ru.spbstu.parsers.util.asCollection

interface Source<out T> {
    val current: T
    operator fun hasNext(): Boolean
    fun advance(): Source<T>

    fun <C: MutableCollection<@UnsafeVariance T>> takeTo(accumulator: C, n: Int) = takeDefault(accumulator, n)
    fun drop(n: Int): Source<T> = dropDefault(n)
    companion object {
        fun <T, C: MutableCollection<@UnsafeVariance T>> Source<T>.takeDefault(accumulator: C, n: Int) {
            var self = this
            repeat(n) {
                accumulator.add(self.current)
                self = self.advance()
            }
        }

        fun <T, S: Source<T>> S.dropDefault(n: Int): S {
            var self = this
            repeat(n) { self = self.advance() as S }
            return self
        }
    }
}

val <T> Source<T>.currentOrNull: T?
    get() = if (hasNext()) current else null

class SourceIterator<T>(var source: Source<T>): Iterator<T> {
    override fun hasNext(): Boolean = source.hasNext()
    override fun next(): T {
        val res = source.current
        source = source.advance()
        return res
    }
}

operator fun <T> Source<T>.iterator() = SourceIterator(this)

class SourceAsCharSequence(val source: Source<Char>,
                           override val length: Int = Int.MAX_VALUE): CharSequence {
    override fun get(index: Int): Char =
        when (index) {
            0 -> source.current
            else -> source.drop(index).current
        }
    override fun subSequence(startIndex: Int, endIndex: Int): SourceAsCharSequence {
        require(startIndex > 0)
        require(startIndex < endIndex)
        return SourceAsCharSequence(source.drop(startIndex), endIndex - startIndex)
    }

    override fun toString(): String = when (length) {
        Int.MAX_VALUE -> "${source.current}..."
        else -> CharArray(length) { get(it) }.concatToString()
    }
}

fun Source<Char>.asCharSequence(): CharSequence = SourceAsCharSequence(this)

fun Source<Char>.takeString(length: Int): String {
    val sb = StringBuilder()
    takeTo(sb.asCollection(), length)
    return "$sb"
}

class ParsedSource<T, R>(val input: Input<T>, val parser: Parser<T, R>, val ignored: Parser<T, Unit>? = null): Source<R> {
    val actualIgnored = ignored?.let { many(it) }
    val newResult: ParseResult<T, R> by lazy {
        val tryIgnoring = actualIgnored?.invoke(input)?.restOrThrow ?: input
        parser(tryIgnoring)
    }

    override val current: R
        get() = newResult.resultOrThrow

    override fun hasNext(): Boolean = input.hasNext() && newResult is ParseSuccess

    override fun advance(): ParsedSource<T, R> = ParsedSource(newResult.restOrThrow, parser, ignored)
    override fun drop(n: Int): ParsedSource<T, R> = dropDefault(n)
}

data class StringSource(val data: String, val currentIndex: Int = 0): Source<Char> {
    override val current: Char
        get() = data[currentIndex]

    override fun hasNext(): Boolean = currentIndex < data.length

    override fun advance(): Source<Char> = copy(currentIndex = currentIndex + 1)
    override fun drop(n: Int): Source<Char> = copy(currentIndex = currentIndex + n)

    override fun <C : MutableCollection<Char>> takeTo(accumulator: C, n: Int) {
        for (i in currentIndex until (currentIndex + n).coerceAtMost(data.length)) {
            accumulator += data[i]
        }
    }

    override fun toString(): String = data.substring(currentIndex)
}
