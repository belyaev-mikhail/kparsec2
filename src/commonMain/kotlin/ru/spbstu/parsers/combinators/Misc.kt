package ru.spbstu.parsers.combinators

import ru.spbstu.*
import kotlin.experimental.ExperimentalTypeInference

infix fun <T, A> Parser<T, A>.named(name: String) = object : Parser<T, A> by this {
    override fun toString(): String = name
}

fun <T, A> namedParser(name: String, parser: Parser<T, A>) = object : Parser<T, A> by parser {
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
