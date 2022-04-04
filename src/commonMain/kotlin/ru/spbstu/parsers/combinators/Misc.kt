package ru.spbstu.parsers.combinators

import ru.spbstu.*
import ru.spbstu.wheels.NoStackThrowable
import kotlin.experimental.ExperimentalTypeInference

infix fun <T, A> Parser<T, A>.named(name: String) = namedParser(name, this)

fun <T, A> namedParser(name: String, parser: Parser<T, A>): Parser<T, A> =
    object : Parser<T, A> by parser, AbstractNamedParser<T, A>(name) {}

inline fun <T, A> namedParser(
    name: String,
    crossinline parser: NamedParser<T, A>.(input: Input<T>) -> ParseResult<@UnsafeVariance T, A>
): Parser<T, A> = object : AbstractNamedParser<T, A>(name) {
    override fun invoke(input: Input<T>): ParseResult<T, A> = parser(input)
}

fun <T, A> Parser<T, A>.ignoreResult(): Parser<T, Unit> = map { Unit }

class DoScope<T>(input: Input<T>) {
    var input: Input<T> = input
        private set

    operator fun <A> Parser<T, A>.invoke(): A {
        val res = this(input)
        when(res) {
            is NoSuccess -> throw ParserDoException(res)
            is ParseSuccess -> {}
        }
        input = res.rest
        return res.result
    }

    fun <A> parse(parser: Parser<T, A>): A = parser()
    fun <A> tryParse(parser: Parser<T, A>): A? {
        val res = parser(input)
        when (res) {
            is ParseError -> throw ParserDoException(res)
            is ParseFailure -> return null
            is ParseSuccess -> {
                input = res.rest
                return res.result
            }

        }
    }
}

data class ParserDoException(val result: NoSuccess): NoStackThrowable()

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
inline fun <T, R> parserDo(crossinline body: DoScope<T>.() -> R): Parser<T, R> = Parser { input ->
    val scope = DoScope(input)
    try {
        val result = scope.body()
        ParseSuccess(result = result, rest = scope.input)
    } catch (ex: ParserDoException) {
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
