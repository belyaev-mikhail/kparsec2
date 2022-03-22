package ru.spbstu.parsers.combinators

import ru.spbstu.*
import kotlin.experimental.ExperimentalTypeInference

infix fun <T, A> Parser<T, A>.named(name: String) = namedParser(name, this)

fun <T, A> namedParser(name: String, parser: Parser<T, A>): Parser<T, A> =
    object : Parser<T, A> by parser, NamedParser<T, A> {
        override val name: String
            get() = name
    }

inline fun <T, A> namedParser(
    name: String,
    crossinline parser: (input: Input<T>) -> ParseResult<@UnsafeVariance T, A>
): Parser<T, A> = object : NamedParser<T, A> {
    override val name: String
        get() = name
    override fun invoke(input: Input<T>): ParseResult<T, A> = parser(input)
    override fun toString(): String = name
}

fun <T, A> Parser<T, A>.ignoreResult(): Parser<T, Unit> = map { Unit }

class DoScope<T>(input: Input<T>) {
    var input: Input<T> = input
        private set

    operator fun <A> Parser<T, A>.invoke(): A {
        val res = this(input).successOrThrow
        input = res.rest
        return res.result
    }

    fun <A> parse(parser: Parser<T, A>): A = parser()
    fun <A> tryParse(parser: Parser<T, A>): A? {
        val res = parser(input)
        when (res) {
            is ParseError -> throw ParseException(res)
            is ParseFailure -> return null
            is ParseSuccess -> {
                input = res.rest
                return res.result
            }

        }
    }
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
inline fun <T, R> parserDo(crossinline body: DoScope<T>.() -> R): Parser<T, R> = Parser { input ->
    val scope = DoScope(input)
    try {
        val result = scope.body()
        ParseSuccess(result = result, rest = scope.input)
    } catch (ex: ParseException) {
        ex.result
    }
}

fun <T, R> lazyParser(body: () -> Parser<T, R>): Parser<T, R> {
    val defer by lazy(body)
    return Parser { defer(it) }
}

fun <T, R> recursive(body: (Parser<T, R>) -> Parser<T, R>): Parser<T, R> {
    var capture: Parser<T, R>? = null
    val defer by lazy { body(capture!!) }
    capture = Parser { defer(it) }
    return capture
}
