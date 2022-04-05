package ru.spbstu.parsers.packrat

import ru.spbstu.AbstractNamedParser
import ru.spbstu.Input
import ru.spbstu.ParseResult
import ru.spbstu.Parser

class PackratParser<T, R>(val inner: Parser<T, R>): AbstractNamedParser<T, R>("${inner}"), Parser<T, R> {
    override fun invoke(input: Input<T>): ParseResult<T, R> = when (input) {
        !is PackratInput -> inner(input)
        else -> input.getOrPut(inner) { inner(input) }
    }
}

fun <T, R> packrat(parser: Parser<T, R>): Parser<T, R> = PackratParser(parser)
