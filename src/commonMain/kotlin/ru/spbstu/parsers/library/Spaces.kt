package ru.spbstu.parsers.library

import ru.spbstu.parsers.combinators.optional
import ru.spbstu.parsers.dsl.or
import ru.spbstu.parsers.dsl.plus
import ru.spbstu.parsers.dsl.unaryMinus
import ru.spbstu.parsers.combinators.manyOneTokens
import ru.spbstu.parsers.combinators.manyTokens
import ru.spbstu.parsers.combinators.token

object Spaces {
    val whitespace = token<Char>("whitespace") { it.isWhitespace() }
    val spaces = manyTokens<Char>("whitespaces") { it.isWhitespace() }
    val oneOrMoreSpaces = manyOneTokens<Char>("whitespaces") { it.isWhitespace() }

    val newline = -token('\n') or (-token('\r') + -optional(token('\n')))
}