package ru.spbstu.parsers.library

import ru.spbstu.parsers.combinators.manyOne
import ru.spbstu.parsers.combinators.optional
import ru.spbstu.parsers.dsl.or
import ru.spbstu.parsers.dsl.plus
import ru.spbstu.parsers.dsl.unaryMinus
import ru.spbstu.parsers.manyOneTokens
import ru.spbstu.parsers.manyTokens
import ru.spbstu.parsers.token

object Spaces {
    val whitespace = token<Char>("whitespace") { it.isWhitespace() }
    val spaces = manyTokens<Char>("whitespaces") { it.isWhitespace() }
    val oneOrMoreSpaces = manyOneTokens<Char>("whitespaces") { it.isWhitespace() }

    val newline = -token('\n') or (-token('\r') + -optional(token('\n')))
}