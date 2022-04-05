package ru.spbstu.parsers.packrat

import ru.spbstu.Input
import ru.spbstu.Location
import ru.spbstu.ParseResult
import ru.spbstu.Parser

class PackratInput<T>(val inner: Input<T>): Input<T>(inner.source) {
    override val location: Location<*>
        get() = inner.location
    override fun advance(): Input<T> = PackratInput(inner.advance())
    override fun drop(n: Int): Input<T> = PackratInput(inner.drop(n))

    override fun equals(other: Any?): Boolean =
        other is PackratInput<*> && inner.equals(other.inner) || inner.equals(other)
    override fun hashCode(): Int = inner.hashCode()
    override fun toString(): String = inner.toString()

    @PublishedApi
    internal val packratTable = mutableMapOf<Parser<T, *>, ParseResult<T, *>>()

    inline fun <R> getOrPut(parser: Parser<T, R>, body: () -> ParseResult<T, R>): ParseResult<T, R> =
        @Suppress("UNCHECKED_CAST")
        (packratTable.getOrPut(parser, body) as ParseResult<T, R>)
}
