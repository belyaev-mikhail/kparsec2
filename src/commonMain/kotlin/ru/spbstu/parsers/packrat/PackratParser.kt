package ru.spbstu.parsers.packrat

import ru.spbstu.*

class PackratParser<T, R>(val inner: Parser<T, R>): AbstractNamedParser<T, R>("${inner}"), Parser<T, R> {
    override fun invoke(input: Input<T>): ParseResult<T, R> = when (input) {
        !is CompoundInput -> invoke(packrat(input))
        else -> {
            val packrat: PackratInput<T> by input
            packrat.getOrPut(inner) { inner(input) }
        }
    }
}

fun <T, R> packrat(parser: Parser<T, R>): Parser<T, R> = PackratParser(parser)
