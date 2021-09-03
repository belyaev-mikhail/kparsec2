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

abstract class Input<out T>(val source: Source<T>): Source<T> by source {
    abstract val location: Location<*>
}

class SimpleInput<out T>(source: Source<T>, override val location: Location<@UnsafeVariance T>): Input<T>(source) {
    override fun advance(): SimpleInput<T> {
        val current = source.current
        return SimpleInput(source.advance(), location(current))
    }

    override fun drop(n: Int): SimpleInput<T> {
        if (location.canSkip) {
            return SimpleInput(source.drop(n), location.skip(n))
        } else {
            return dropDefault(n)
        }
    }
}

fun <T> Input<T>.failure(expected: String, actual: String): Failure = Failure(expected, actual, location)
fun <T> Input<T>.error(expected: String, actual: String): Error = Error(expected, actual, location)

class ParsedSource<T, R>(val input: Input<T>, val parser: Parser<T, R>): Source<R> {
    val newResult: ParseResult<T, R> by lazy { parser(input) }

    override val current: R
        get() = newResult.resultOrThrow

    override fun hasNext(): Boolean = input.hasNext() && newResult is ParseSuccess

    override fun advance(): Source<R> = ParsedSource(newResult.restOrThrow, parser)
}

class ParsedInput<T, R>(input: Input<T>,
                        parser: Parser<T, R>): Input<R>(ParsedSource(input, parser)) {
    override val location: Location<*>
        get() = (source as ParsedSource<*, *>).input.location
}
