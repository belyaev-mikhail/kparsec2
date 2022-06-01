package ru.spbstu.kparsec2

import ru.spbstu.kparsec2.parsers.combinators.*
import ru.spbstu.kparsec2.parsers.dsl.*
import ru.spbstu.kparsec2.util.quoteChar
import ru.spbstu.wheels.joinToString
import kotlin.test.Test
import kotlin.test.assertEquals

object JsonTokens {
    val boolean = sequence("true").map { BooleanLiteral(true) } or
            sequence("false").map { BooleanLiteral(false) }

    // rfc7159 number grammar
    object NumberGrammar {
        val minus = token('-')
        val plus = token('+')
        val zero = token('0')
        val decimalPoint = token('.')
        val digit19 = oneOf('1'..'9')
        val digit = oneOf('0'..'9')
        val digits: Parser<Char, List<Char>> = many(digit)
        val e = oneOf("eE")
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

    val token = run {
        val lookupTable = arrayOfNulls<Parser<Char, Token>>(256)

        for ((c, p) in SyntaxToken.charMap) {
            lookupTable[c.code] = any<Char>().map { p }
        }
        lookupTable['"'.code] = string
        lookupTable['t'.code] = boolean
        lookupTable['f'.code] = boolean
        lookupTable['n'.code] = nullLit
        for (c in "01293456789eE+-") lookupTable[c.code] = number

        val fallback = oneOf(lookupTable.filterNotNullTo(mutableSetOf()))

        peekChoice<Char, Token> {
            lookupTable.getOrNull(it.code) ?:
                when {
                    it.isDigit() -> number // it may still be some weird kind of digit not in '0'..'9'
                    else -> failure("$fallback", quoteChar(it))
                }
        }
    }

    fun jsonTokenInput(s: String) = parsedInput(stringInput(s), token, space)
}

object JsonGrammar {
    val expr: Parser<Token, JsonExpr> = lazyParser {
        arr or obj or simpleExpr
    }
    fun tok(ch: Char): Parser<Token, Unit> = -SyntaxToken.valueOf(ch)!!

    val simpleExpr =
        token<Token, Literal>()

    val arrayBody =
        expr sepBy tok(',') map ::JsonArray

    val arr =
        tok('[') + arrayBody + tok(']')

    val objectKey =
        token<Token, StringLiteral>() map StringLiteral::data

    val objEntry =
        objectKey + tok(':') zipTo expr map ::jsonObjectEntry

    val objectBody =
        objEntry sepBy tok(',') map ::JsonObject

    val obj: Parser<Token, JsonExpr> =
        tok('{') + objectBody + tok('}')
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