package ru.spbstu.kparsec2

import ru.spbstu.kparsec2.parsers.combinators.*
import ru.spbstu.kparsec2.parsers.dsl.*
import ru.spbstu.wheels.joinToString

// this is not a complete JSON implementation, but pretty close
sealed interface Token
sealed interface Literal: Token, JsonExpr {
    val data: Any?
}
data class StringLiteral(override val data: String): Literal {
    override fun toString(): String = "\"$data\""
}
data class NumberLiteral(override val data: Double): Literal {
    override fun toString(): String = "$data"
}
data class BooleanLiteral(override val data: Boolean): Literal {
    override fun toString(): String = "$data"
}
object NullLiteral: Literal {
    override val data: Nothing? = null
    override fun toString(): String = "null"
}
enum class SyntaxToken(val rep: Char): Token, Parser<Token, Token> {
    LBracket('['), RBracket(']'),
    LBrace('{'), RBrace('}'), Comma(','), Colon(':');

    val parser = token<Token> { this == it }
    override fun invoke(input: Input<Token>): ParseResult<Token, Token> = parser.invoke(input)

    companion object {
        val charMap: Map<Char, SyntaxToken> = values().associateBy { it.rep }
        fun valueOf(ch: Char) = charMap[ch]
    }
}

sealed interface JsonExpr
data class JsonArray(val data: List<JsonExpr>): JsonExpr {
    constructor(vararg data: JsonExpr): this(data.asList())
    override fun toString(): String = data.toString()
}
data class JsonObject(val data: Map<String, JsonExpr>): JsonExpr {
    constructor(vararg data: Pair<String, JsonExpr>): this(data.toMap())

    override fun toString(): String = data.joinToString(prefix = "{", postfix = "}") { k, v -> "\"$k\": $v" }
}
