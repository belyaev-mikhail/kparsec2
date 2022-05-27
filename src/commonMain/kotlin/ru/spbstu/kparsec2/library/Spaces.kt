package ru.spbstu.kparsec2.parsers.library

import ru.spbstu.kparsec2.parsers.combinators.manyAsString
import ru.spbstu.kparsec2.parsers.combinators.manyOneAsString
import ru.spbstu.kparsec2.parsers.combinators.optional
import ru.spbstu.kparsec2.parsers.combinators.token
import ru.spbstu.kparsec2.parsers.dsl.or
import ru.spbstu.kparsec2.parsers.dsl.plus
import ru.spbstu.kparsec2.parsers.dsl.unaryMinus

object Spaces {
    val whitespace = token<Char>("whitespace") { it.isWhitespace() }
    val spaces = manyAsString("whitespaces") { it.isWhitespace() }
    val oneOrMoreSpaces = manyOneAsString("whitespaces") { it.isWhitespace() }

    val newline = -token('\n') or (-token('\r') + -optional(token('\n')))
}