package ru.spbstu.kparsec2

import ru.spbstu.kparsec2.parsers.combinators.*
import ru.spbstu.kparsec2.parsers.dsl.*
import ru.spbstu.wheels.joinToString
import kotlin.test.Test
import kotlin.test.assertEquals

object JsonTokens {
    val boolean = sequence("true").map { BooleanLiteral(true) } or
            sequence("false").map { BooleanLiteral(false) }

    // rfc7159 number grammar
    object NumberGrammar {
        val minus: Parser<Char, Char> = token('-')
        val plus: Parser<Char, Char> = token('+')
        val zero: Parser<Char, Char> = token('0')
        val decimalPoint: Parser<Char, Char> = token('.')
        val digit19: Parser<Char, Char> = oneOf('1'..'9')
        val digit = token(expectedString = "digit") { it: Char -> it.isDigit() }
        val digits: Parser<Char, List<Char>> = many(digit)
        val e: Parser<Char, Char> = oneOf("eE")
        val exp: Parser<Char, List<Char>> = e + (minus or plus) + digits
        val frac: Parser<Char, List<Char>> = decimalPoint + digits
        val int: Parser<Char, List<Char>> = zero.map { listOf(it) } or (digit19 + digits)

        val number: Parser<Char, Double> = zipWith(
            optional(minus),
            int,
            optional(frac),
            optional(exp)
        ) { m, i, f, e ->
            buildString {
                m?.let(::append)
                i.forEach(::append)
                f?.forEach(::append)
                e?.forEach(::append)
            }.toDouble()
        }
    }

    val number = NumberGrammar.number.map(::NumberLiteral)

    object StringGrammar {
        val escapables = "bfrnt\"\\/".toSet()

        val quote = token('\"')
        val escape = token('\\')

        val unescaped = token<Char> { it: Char -> it !in "\"\\" }
        val escapee = oneOf(escapables).map {
            when(it) {
                'b' -> '\b'
                'f' -> Char(0xC)
                'r' -> '\r'
                'n' -> '\n'
                't' -> '\t'
                else -> it
            }
        }
        val uescapee = -oneOf("uU") + (oneOf(('0'..'9') + ('a'..'f') + ('A'..'F')) * 4).map {
            it.joinToString("").toInt(16).toChar()
        }
        val char = unescaped or (-escape + (escapee or uescapee))
        val string = -quote + manyAsString(char) + -quote
    }
    val string = StringGrammar.string.map { StringLiteral(it.toString()) }

    val nullLit = sequence("null").map { NullLiteral }
    val syntaxToken = oneOf(SyntaxToken.charMap.keys).map { SyntaxToken.valueOf(it)!! }

    val space = -token<Char> { it.isWhitespace() }

    val token = syntaxToken or string or number or boolean or nullLit

    fun jsonTokenInput(s: String) = parsedInput(stringInput(s), token, space)
}

object JsonGrammar {
    val expr: Parser<Token, JsonExpr> = lazyParser {
        arr or obj or simpleExpr
    }
    fun syntaxToken(ch: Char): Parser<Token, Unit> = -SyntaxToken.valueOf(ch)!!

    val simpleExpr: Parser<Token, Literal> = token<Token, Literal>()
    val manyExpr: Parser<Token, List<JsonExpr>> = expr sepBy syntaxToken(',')
    val arr: Parser<Token, JsonExpr> =
        syntaxToken('[') + manyExpr.map(::JsonArray) + syntaxToken(']')
    val objEntry: Parser<Token, Pair<String, JsonExpr>>  = zipWith(
        token<Token, StringLiteral>(),
        -SyntaxToken.Colon,
        expr
    ) { key: StringLiteral, _, value: JsonExpr -> key.data to value }

    val obj: Parser<Token, JsonExpr> = syntaxToken('{') +
            (objEntry sepBy -syntaxToken(',')).map { JsonObject(it.toMap()) } +
            syntaxToken('}')
}

class JsonParserTest {
    val JSON: Parser<Char, JsonExpr> = liftParser(JsonGrammar.expr, JsonTokens.token, JsonTokens.space)

    @Test
    fun testJsonParsing() {
        assertEquals(
            StringLiteral("Hello"),
            JSON("\"Hello\"").resultOrThrow
        )

        assertEquals(
            NumberLiteral(2.0),
            JSON(" 2.0").resultOrThrow
        )

        //println(JsonGrammar.expr(JsonTokens.jsonTokenInput(""" { "x": [] }""")))

        assertEquals(
            JsonArray(JsonArray(), NumberLiteral(2.0)),
            JSON(" [  [], 2.0]").resultOrThrow
        )

    }


}