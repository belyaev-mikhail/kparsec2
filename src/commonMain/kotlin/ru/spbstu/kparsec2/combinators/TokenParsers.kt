package ru.spbstu.kparsec2.parsers.combinators

import kotlinx.warnings.Warnings
import ru.spbstu.kparsec2.*

/*
* Marker interface marking that we always only consume 1 token
* */
interface SingleTokenParser<T> : Parser<T, T>

internal object AnyParser: SingleTokenParser<Any?>, NamedParser<Any?, Any?> {
    override val name: String
        get() = "<any>"

    override fun toString(): String = name

    override fun invoke(input: Input<Any?>): ParseResult<Any?, Any?> {
        if (!input.hasNext()) return input.failure()
        val current = input.current
        return ParseSuccess(input.advance(), current)
    }
}

fun <T> any(): Parser<T, T> = @Suppress(Warnings.UNCHECKED_CAST) (AnyParser as Parser<T, T>)

abstract class PredicateTokenParser<T>(name: String): SingleTokenParser<T>, AbstractNamedParser<T, T>(name) {
    abstract fun isValid(token: T): Boolean
    override fun invoke(input: Input<T>): ParseResult<T, T> {
        if (!input.hasNext()) return input.failure()
        val current = input.current
        return when {
            isValid(current) -> ParseSuccess(input.advance(), current)
            else -> input.failure()
        }
    }

    // avoiding KT-52553 KJS / IR: diamond hierarchy with super.toString produces stack overflow in runtime
    override fun toString(): String = super<AbstractNamedParser>.toString()
}

inline fun <T> token(expectedString: String = "<predicate>", crossinline predicate: (T) -> Boolean): Parser<T, T> =
    object : PredicateTokenParser<T>(expectedString) {
        override fun isValid(token: T): Boolean = predicate(token)
    }

data class TokenEqualityParser<T>(val expected: T): PredicateTokenParser<T>("$expected") {
    override fun isValid(token: T): Boolean = token == expected
    override fun toString(): String = name
}

fun <T> token(value: T): Parser<T, T> = TokenEqualityParser(value)

@Suppress(Warnings.UNCHECKED_CAST)
inline fun <T, reified S: T> token(expectedString: String = "${S::class.simpleName}"): Parser<T, S> =
    token<T>(expectedString) { it is S } as Parser<T, S>

data class TokenOneOfParser<T>(val expected: Set<T>): PredicateTokenParser<T>(
    expected.joinToString("", prefix = "<one of [", postfix = "]>")
) {
    override fun isValid(token: T): Boolean = token in expected
    override fun toString(): String = super.toString()
}

fun <T> oneOf(tokens: Set<T>): Parser<T, T> = when(tokens.size) {
    1 -> token(tokens.single())
    else -> TokenOneOfParser(tokens)
}

fun <T> oneOf(vararg tokens: T): Parser<T, T> = when(tokens.size) {
    1 -> token(tokens.single())
    else -> oneOf(tokens.toSet())
}
fun <T> oneOf(tokens: Iterable<T>): Parser<T, T> = oneOf(tokens.toSet())

fun <T> sequence(iterator: Iterable<T>, expectedString: String = "<predicate>"): Parser<T, List<T>> =
    namedParser(expectedString) {
        val result = mutableListOf<T>()
        var self = it
        for (element in iterator) {
            when (val current = self.current) {
                element -> {
                    result += current
                    self = self.advance()
                }
                else -> {
                    result += current
                    return@namedParser it.failure(name, result)
                }
            }
        }
        ParseSuccess(self, result)
    }

fun <T> sequence(vararg tokens: T): Parser<T, List<T>> = when(tokens.size) {
    1 -> token(tokens.single()).map { listOf(it) }
    else -> sequence(tokens.asList())
}
fun <T> sequence(tokens: Iterable<T>): Parser<T, List<T>> = when(tokens) {
    is Collection -> sequence(tokens as Collection<T>)
    else -> sequence(tokens, expectedString = tokens.joinToString())
}
fun <T> sequence(tokens: Collection<T>): Parser<T, List<T>> = when(tokens.size) {
    0 -> success(listOf())
    1 -> token(tokens.single()).map { listOf(it) }
    else -> namedParser({ tokens.joinToString(" + ") }) {
        val sourceTokens = ArrayList<T>(tokens.size)
        it.takeTo(sourceTokens, tokens.size)
        if (sourceTokens != tokens) it.failure(name, sourceTokens)
        else ParseSuccess(it.drop(tokens.size), sourceTokens)
    }
}

inline fun <T> manyOneTokens(
                    expectedString: String = "<predicate>",
                    crossinline pred: (T) -> Boolean): Parser<T, List<T>> =
    manyTokens(expectedString, pred).filter(expectedString) { it.size > 1 }

inline fun <T> manyTokens(
    expectedString: String = "<predicate>",
    crossinline pred: (T) -> Boolean): Parser<T, List<T>> =
    namedParser(expectedString) {
        val result = mutableListOf<T>()
        var self = it
        while (self.hasNext()) {
            val current = self.current
            when {
                pred(current) -> {
                    result += current
                    self = self.advance()
                }
                else -> break
            }
        }
        ParseSuccess(self, result as List<T>)
    }

inline fun <T, R> choice(crossinline body: (T) -> Parser<T, R>): Parser<T, R> = Parser {
    body(it.current).invoke(it.advance())
}

inline fun <T, R> peekChoice(crossinline body: (T) -> Parser<T, R>): Parser<T, R> = Parser {
    body(it.current).invoke(it)
}

data class TokenInequalityParser<T>(val token: T): PredicateTokenParser<T>("!$token") {
    override fun isValid(token: T): Boolean = token != this.token
    override fun toString(): String = super.toString()
}

fun <T> notToken(value: T): Parser<T, T> = TokenInequalityParser(value)

data class TokenNotOneOfParser<T>(val expected: Set<T>): PredicateTokenParser<T>(
    expected.joinToString("", prefix = "<not one of [", postfix = "]>")
) {
    override fun isValid(token: T): Boolean = token !in expected
    override fun toString(): String = super.toString()
}

fun <T> notOneOf(tokens: Set<T>): Parser<T, T> = when(tokens.size) {
    1 -> notToken(tokens.single())
    else -> TokenNotOneOfParser(tokens)
}

fun <T> notOneOf(vararg tokens: T): Parser<T, T> = when(tokens.size) {
    1 -> notToken(tokens.single())
    else -> notOneOf(tokens.toSet())
}
fun <T> notOneOf(tokens: Iterable<T>): Parser<T, T> = notOneOf(tokens.toSet())
