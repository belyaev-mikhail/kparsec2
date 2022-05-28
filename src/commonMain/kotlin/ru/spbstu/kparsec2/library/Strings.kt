package ru.spbstu.kparsec2.library

import ru.spbstu.kparsec2.Parser
import ru.spbstu.kparsec2.parsers.combinators.*
import ru.spbstu.kparsec2.parsers.dsl.*
import ru.spbstu.kparsec2.parsers.library.Numbers

object Strings {

    val defaultEscapes: Map<Char, Char> = mapOf(
        'r' to '\r',
        'n' to '\n',
        't' to '\t',
        'b' to '\b',
        '0' to Char(0),
    )

    val defaultComplexEscapes: Map<Char, Parser<Char, Char>> = mapOf(
        'x' to (Numbers.hexDigit * 2).map { it.joinToString("").toInt(16).toChar() },
        'u' to (Numbers.hexDigit * 4).map { it.joinToString("").toInt(16).toChar() }
    )

    private fun simpleChar(vararg invalid: Char): Parser<Char, Char> {
        val escapeable = buildSet { invalid.forEach { add(it) }; add('\\') }
        return token { it !in escapeable }
    }

    private fun escapedChar(
        escapeable: Map<Char, Char> = defaultEscapes,
        complexEscapeable: Map<Char, Parser<Char, Char>> = defaultComplexEscapes,
        vararg invalid: Char
    ): Parser<Char, Char> {
        val escMap = escapeable.toMutableMap()
        for (ch in invalid) escMap[ch] = ch
        escMap['\\'] = '\\'
        val charPart = oneOf(escMap.keys).mapNotNull { escMap[it] } or
                oneOf(complexEscapeable.keys).flatMap { complexEscapeable[it]!! }
        return -token('\\') + charPart
    }

    fun quotedString(
        openQuote: Char = '"',
        closeQuote: Char = '"',
        escapeable: Map<Char, Char> = defaultEscapes,
        complexEscapeable: Map<Char, Parser<Char, Char>> = defaultComplexEscapes
    ): Parser<Char, CharSequence> {
        val single = simpleChar(openQuote, closeQuote) or
                escapedChar(escapeable, complexEscapeable, openQuote, closeQuote)
        return -token(openQuote) + manyAsString(single) + -token(closeQuote)
    }

    fun quotedChar(
        openQuote: Char = '\'',
        closeQuote: Char = '\'',
        escapeable: Map<Char, Char> = defaultEscapes,
        complexEscapeable: Map<Char, Parser<Char, Char>> = defaultComplexEscapes
    ): Parser<Char, Char> {
        val single = simpleChar(openQuote, closeQuote) or
                escapedChar(escapeable, complexEscapeable, openQuote, closeQuote)
        return -token(openQuote) + single + -token(closeQuote)
    }

    val javaStringLiteral = quotedString()
    val javaCharLiteral = quotedChar()

}