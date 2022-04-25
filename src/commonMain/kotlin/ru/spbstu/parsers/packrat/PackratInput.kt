package ru.spbstu.parsers.packrat

import ru.spbstu.*

class PackratInput<T>: InputComponent {
    @PublishedApi
    internal val packratTable = mutableMapOf<Parser<T, *>, ParseResult<T, *>>()

    inline fun <R> getOrPut(parser: Parser<T, R>, body: () -> ParseResult<T, R>): ParseResult<T, R> =
        @Suppress("UNCHECKED_CAST")
        (packratTable.getOrPut(parser, body) as ParseResult<T, R>)

    companion object {
        inline fun <reified T> Key(): InputComponent.Key<PackratInput<T>> = InputComponent.Key()
    }
}

fun <T> packrat(input: Input<T>): CompoundInput<T> = input.putComponentIfAbsent { PackratInput<T>() }
