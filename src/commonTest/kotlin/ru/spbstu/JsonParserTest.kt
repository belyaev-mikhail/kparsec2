package ru.spbstu

import ru.spbstu.parsers.dsl.*
import ru.spbstu.parsers.*
import ru.spbstu.parsers.combinators.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonParserTest {
    // this is not a complete JSON implementation, but pretty close
    sealed interface Token
    sealed interface Literal<T>: Token, JsonExpr {
        val data: T
    }
    data class StringLiteral(override val data: String): Literal<String>
    data class NumberLiteral(override val data: Double): Literal<Double>
    data class BooleanLiteral(override val data: Boolean): Literal<Boolean>
    object NullLiteral: Literal<Nothing?> {
        override val data: Nothing? = null
    }
    enum class SyntaxToken(val rep: Char): Token {
        LBracket('['), RBracket(']'),
        LBrace('{'), RBrace('}'), Comma(','), Colon(':');

        companion object {
            val charMap: Map<Char, SyntaxToken> = values().associateBy { it.rep }
            fun valueOf(ch: Char) = charMap[ch]
        }
    }

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
        val string = StringGrammar.string.map(::StringLiteral)

        val nullLit = sequence("null").map { NullLiteral }
        val syntaxToken = oneOf(SyntaxToken.charMap.keys).map { SyntaxToken.valueOf(it)!! }

        val space = -token<Char> { it.isWhitespace() }

        val token = syntaxToken or string or number or boolean or nullLit

        fun jsonTokenInput(s: String) = parsedInput(stringInput(s), token, space)
    }

    sealed interface JsonExpr
    data class JsonArray(val data: List<JsonExpr>): JsonExpr {
        constructor(vararg data: JsonExpr): this(data.asList())
    }
    data class JsonObject(val data: Map<String, JsonExpr>): JsonExpr {
        constructor(vararg data: Pair<String, JsonExpr>): this(data.toMap())
    }

    object JsonGrammar {
        val expr: Parser<Token, JsonExpr> = lazyParser {
            arr or obj or simpleExpr
        }
        fun syntaxToken(ch: Char): Parser<Token, Unit> = -token<Token>(SyntaxToken.valueOf(ch)!!)

        val simpleExpr = token<Token, Literal<*>>()
        val manyExpr = expr sepBy -syntaxToken(',')
        val arr = -syntaxToken('[') + manyExpr.map(::JsonArray) + -syntaxToken(']')
        val objEntry = zipWith(
            token<Token, StringLiteral>(),
            token(SyntaxToken.Colon),
            expr) { key, _, value -> key.data to value }

        val obj = -syntaxToken('{') +
                (objEntry sepBy -syntaxToken(',')).map { JsonObject(it.toMap()) } +
                -syntaxToken('}')
    }

    @Test
    fun testJsonParsing() {
        val JSON = liftParser(JsonGrammar.expr, JsonTokens.token, JsonTokens.space)

        assertEquals(
            StringLiteral("Hello"),
            JSON("\"Hello\"").resultOrThrow
        )

        assertEquals(
            NumberLiteral(2.0),
            JSON(" 2.0").resultOrThrow
        )

        println(JsonGrammar.expr(JsonTokens.jsonTokenInput(""" { "x": [] }""")))

        assertEquals(
            JsonArray(JsonArray(), NumberLiteral(2.0)),
            JSON(" [  [], 2.0]").resultOrThrow
        )

    }


}