package ru.spbstu

import ru.spbstu.Source.Companion.dropDefault

interface Source<out T> {
    val current: T
    operator fun hasNext(): Boolean
    fun advance(): Source<T>

    fun drop(n: Int): Source<T> = dropDefault(n)
    companion object {
        fun <T, S: Source<T>> S.dropDefault(n: Int): S {
            var self = this
            repeat(n) { self = self.advance() as S }
            return self
        }
    }
}

class SourceIterator<T>(var source: Source<T>): Iterator<T> {
    override fun hasNext(): Boolean = source.hasNext()
    override fun next(): T {
        source = source.advance()
        return source.current
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

class ParsedSource<T, R>(val input: Input<T>, val parser: Parser<T, R>): Source<R> {
    val newResult: ParseResult<T, R> by lazy { parser(input) }

    override val current: R
        get() = newResult.resultOrThrow

    override fun hasNext(): Boolean = input.hasNext() && newResult is ParseSuccess

    override fun advance(): ParsedSource<T, R> = ParsedSource(newResult.restOrThrow, parser)
    override fun drop(n: Int): ParsedSource<T, R> = dropDefault(n)
}

data class StringSource(val data: String, val currentIndex: Int = 0): Source<Char> {
    override val current: Char
        get() = data[currentIndex]

    override fun hasNext(): Boolean = currentIndex < data.length

    override fun advance(): Source<Char> = copy(currentIndex = currentIndex + 1)
    override fun drop(n: Int): Source<Char> = copy(currentIndex = currentIndex + n)
}
