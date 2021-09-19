package ru.spbstu

import ru.spbstu.Source.Companion.dropDefault
import ru.spbstu.parsers.combinators.many
import ru.spbstu.parsers.util.asCollection

interface Source<out T> {
    val current: T
    operator fun hasNext(): Boolean
    fun advance(): Source<T>

    fun <C: MutableCollection<@UnsafeVariance T>> takeTo(accumulator: C, n: Int) = takeDefault(accumulator, n)
    fun drop(n: Int): Source<T> = dropDefault(n)

    val maxLength: Int? get() = null

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

            repeat(n) { self = @Suppress("UNCHECKED_CAST") (self.advance() as S) }
            return self
        }
    }
}

val <T> Source<T>.currentOrNull: T?
    get() = if (hasNext()) current else null

class SourceIterator<T>(private var source: Source<T>): Iterator<T> {
    override fun hasNext(): Boolean = source.hasNext()
    override fun next(): T {
        val res = source.current
        source = source.advance()
        return res
    }
}

operator fun <T> Source<T>.iterator() = SourceIterator(this)

class SourceAsCharSequence(private val source: Source<Char>,
                           override val length: Int = source.maxLength ?: Int.MAX_VALUE): CharSequence {
    override fun get(index: Int): Char =
        when (index) {
            0 -> source.currentOrNull ?: Char(0xFFFF)
            else -> source.drop(index).currentOrNull ?: Char(0)
        }

    override fun subSequence(startIndex: Int, endIndex: Int): SourceAsCharSequence {
        require(startIndex >= 0)
        require(startIndex < endIndex)
        return SourceAsCharSequence(source.drop(startIndex), endIndex - startIndex)
    }

    override fun toString(): String = when (length) {
        Int.MAX_VALUE -> "${source.currentOrNull}..."
        else -> CharArray(length) { get(it) }.concatToString()
    }
}

fun Source<Char>.asCharSequence(): CharSequence = SourceAsCharSequence(this)
fun <T> Source<T>.asTokenSequence(): Sequence<T> = Sequence { iterator() }

fun Source<Char>.takeString(length: Int): String {
    val sb = StringBuilder()
    takeTo(sb.asCollection(), length)
    return "$sb"
}

class ParsedSource<T, R>(
    val input: Input<T>,
    private val parser: Parser<T, R>,
    private val ignored: Parser<T, Unit>? = null
): Source<R> {
    private val actualIgnored = ignored?.let { many(it) }
    private val newResult: ParseResult<T, R> by lazy {
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

    override val maxLength: Int
        get() = data.length - currentIndex

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
