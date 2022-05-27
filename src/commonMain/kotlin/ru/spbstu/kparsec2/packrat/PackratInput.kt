package ru.spbstu.kparsec2.parsers.packrat

import ru.spbstu.kparsec2.*

class PackratInput: InputComponent<PackratInput> {
    @PublishedApi
    internal val packratTable = mutableMapOf<Parser<*, *>, ParseResult<*, *>>()

    inline fun <T, R> getOrPut(parser: Parser<T, R>, body: () -> ParseResult<T, R>): ParseResult<T, R> =
        @Suppress("UNCHECKED_CAST")
        (packratTable.getOrPut(parser, body) as ParseResult<T, R>)

    override fun advance(n: Int): InputComponent<PackratInput> = when (n) {
        0 -> this
        else -> PackratInput()
    }

    companion object {
        inline fun Key(): InputComponent.Key<PackratInput> = InputComponent.Key()
    }

    override fun toString(): String = "Packrat"
}

fun <T> packrat(input: Input<T>): CompoundInput<T> = input.putComponentIfAbsent { PackratInput() }
