package ru.spbstu.kparsec2.parsers.packrat

import ru.spbstu.kparsec2.Input
import ru.spbstu.kparsec2.NamedParser
import ru.spbstu.kparsec2.ParseResult
import ru.spbstu.kparsec2.Parser

class PackratParser<T, R>(val inner: Parser<T, R>): NamedParser<T, R>, Parser<T, R> {
    override fun invoke(input: Input<T>): ParseResult<T, R> = run {
        val pInput = packrat(input)
        val packrat: PackratInput = pInput.getComponent()
        packrat.getOrPut(inner) { inner(pInput) }
    }

    override val name: String
        get() = inner.toString()

    override fun toString(): String = name
}

fun <T, R> packrat(parser: Parser<T, R>): Parser<T, R> = PackratParser(parser)
