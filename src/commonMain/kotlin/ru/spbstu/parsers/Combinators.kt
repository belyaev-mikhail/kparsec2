package ru.spbstu.parsers

import ru.spbstu.*

inline fun <T, A, B> Parser<T, A>.map(crossinline body: (A) -> B): Parser<T, B> = Parser {
    this(it).map(body)
}

inline fun <T, A, B> Parser<T, A>.flatMap(crossinline body: (A) -> Parser<T, B>): Parser<T, B> = Parser {
    this(it).chain { result, rest -> body(result)(rest) }
}

inline fun <T, A> Parser<T, A>.filter(crossinline body: (A) -> Boolean): Parser<T, A> = Parser {
    val trye = this(it)
    trye.flatMap { result -> if (body(result)) trye else it.failure("body", "") }
}

class DoScope<T>(var input: Input<T>) {
    operator fun <A> Parser<T, A>.invoke(): A {
        val res = this(input).successOrThrow
        input = res.rest
        return res.result
    }
}

inline fun <T, R> parserDo(crossinline body: DoScope<T>.() -> R): Parser<T, R> = Parser { input ->
    val scope = DoScope(input)
    try {
        val result = scope.body()
        ParseSuccess(result = result, rest = scope.input)
    } catch (ex: ParseException) {
        ex.result
    }
}

fun <T, A, B, C> zipWith(left: Parser<T, A>, right: Parser<T, B>, body: (A, B) -> C): Parser<T, C> =
    left.flatMap { l -> right.map { r -> body(l, r) } }
