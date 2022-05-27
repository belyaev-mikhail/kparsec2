package ru.spbstu.kparsec2

import ru.spbstu.kparsec2.parsers.combinators.*
import ru.spbstu.kparsec2.parsers.dsl.or
import ru.spbstu.kparsec2.parsers.dsl.plus
import ru.spbstu.kparsec2.parsers.dsl.unaryMinus
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleCompleteParserTest {
    @Test
    fun smokeTest() {
        val inp = stringInput("<<<>>>")
        assertEquals('<', token('<')(inp).resultOrThrow)
        assertEquals(listOf('<', '<', '<'), sequence('<', '<', '<')(inp).resultOrThrow)
    }

    sealed interface Token
    object LPAREN: Token {
        override fun toString(): String = "LPAREN"
    }
    object RPAREN: Token {
        override fun toString(): String = "RPAREN"
    }
    data class Identifier(val contents: String): Token, LispValue
    data class IntegerConstant(val value: Int): Token, LispValue
    data class BoolConstant(val value: Boolean): Token, LispValue

    sealed interface LispExpr
    data class LispList(val children: List<LispExpr>): LispExpr, List<LispExpr> by children {
        constructor(vararg children: LispExpr): this(children.asList())
    }
    sealed interface LispValue: LispExpr

    @Test
    fun lispyLisp() {
        val lparen = token('(').map { LPAREN } named "("
        val rparen = sequence(")").map { RPAREN } named ")"

        val iliteral = manyAsString(token { ch: Char -> ch.isDigit() })
            .map { IntegerConstant(it.toString().toInt()) } named "integer literal"
        val bliteral = oneOf(
            sequence("#t").map { BoolConstant(true) },
            sequence("#f").map { BoolConstant(false) }
        ) named "boolean literal"
        val controlChars = "#() \t\r\n\"\'".toSet()
        val identifier = manyAsString(token { ch: Char -> ch !in controlChars }).map { Identifier(it.toString()) } named "identifier"
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

        val simpleExpr = token<Token> { it is Identifier || it is IntegerConstant || it is BoolConstant }
            .map { it as LispValue }
        val lispExpr = recursive { self: Parser<Token, LispExpr> ->
            simpleExpr or (-token<Token>(LPAREN) + many(self).map(SimpleCompleteParserTest::LispList) + -token<Token>(
                RPAREN
            ))
        }

        val pinput = parsedInput(stringInput("(a (b c))"), tok, spaces)
        println(pinput.iterator().asSequence().joinToString())
        println(lispExpr(pinput))

        assertEquals(
            LispList(
                Identifier("a"),
                LispList(Identifier("b"), Identifier("c"))
            ),
            lispExpr(pinput).resultOrThrow
        )
    }

}