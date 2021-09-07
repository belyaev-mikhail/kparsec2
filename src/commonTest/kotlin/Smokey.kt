package ru.spbstu

import ru.spbstu.parsers.combinators.*
import ru.spbstu.parsers.combinators.oneOf
import ru.spbstu.parsers.dsl.*
import ru.spbstu.parsers.manyAsString
import ru.spbstu.parsers.oneOf
import ru.spbstu.parsers.sequence
import ru.spbstu.parsers.token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Smokey {
    @Test
    fun smokeTest() {
        val inp = stringInput("<<<>>>")
        assertEquals('<', token('<')(inp).resultOrThrow)
        assertEquals(listOf('<', '<', '<'), sequence('<', '<', '<')(inp).resultOrThrow)
    }

    sealed class Token
    object LPAREN: Token() {
        override fun toString(): String = "LPAREN"
    }
    object RPAREN: Token() {
        override fun toString(): String = "RPAREN"
    }
    data class Identifier(val contents: String): Token()
    data class IntegerConstant(val value: Int): Token()
    data class BoolConstant(val value: Boolean): Token()

    @Test
    fun lispyLisp() {
        val lparen = token('(').map { LPAREN } named "("
        val rparen = sequence(")").map { RPAREN } named ")"

        val iliteral = manyAsString(token { ch: Char -> ch.isDigit() })
            .map { IntegerConstant(it.toInt()) } named "integer literal"
        val bliteral = oneOf(
            sequence("#t").map { BoolConstant(true) },
            sequence("#f").map { BoolConstant(false) }
        ) named "boolean literal"
        val controlChars = "#() \t\r\n\"\'".toSet()
        val identifier = manyAsString(token { ch: Char -> ch !in controlChars }).map { Identifier(it) } named "identifier"
        val spaces = many(oneOf(" \t\r\n")).ignoreResult()
        val tok = oneOf(lparen, rparen, identifier, iliteral, bliteral)

        val si = parsedInput(stringInput("(aas ( a )  aasdas))  as ) asa"), tok, spaces)

        assertEquals(
            listOf(
                LPAREN,
                Identifier("aas"),
                LPAREN,
                Identifier("a"),
                RPAREN,
                Identifier("aasdas"),
                RPAREN,
                RPAREN,
                Identifier("as"),
                RPAREN,
                Identifier("asa")
            ),
            si.iterator().asSequence().toList()
        )

        for (token in si) {
            println(token)
        }

    }

}