package ru.spbstu.parsers.combinators

import ru.spbstu.*

inline fun <T, A, B> Parser<T, A>.map(crossinline body: (A) -> B): Parser<T, B> = Parser {
    this(it).map(body)
}

inline fun <T, A, B> Parser<T, A>.flatMap(crossinline body: (A) -> Parser<T, B>): Parser<T, B> = Parser {
    this(it).chain { result, rest -> body(result)(rest) }
}

inline fun <T, A> Parser<T, A>.filter(expected: String? = null, crossinline body: (A) -> Boolean): Parser<T, A> = Parser {
    val trye = this(it)
    trye.flatMap { result -> if (body(result)) trye else it.failure(expected ?: "<predicate>") }
}

inline fun <T, A, B> Parser<T, A>.mapNotNull(expected: String? = null, crossinline body: (A) -> B?): Parser<T, B> = Parser { input ->
    when(val tryout = this(input)) {
        is NoSuccess -> tryout
        is ParseSuccess -> {
            when(val mapped = body(tryout.result)) {
                null -> input.failure(expected ?: "<predicate>")
                else -> tryout.map { mapped }
            }
        }
    }
}

inline fun <T, A, B, C> zipWith(left: Parser<T, A>,
                                right: Parser<T, B>,
                                crossinline body: (A, B) -> C): Parser<T, C> =
    left.flatMap { l -> right.map { r -> body(l, r) } }

inline fun <T, A, B, C, D> zipWith(left: Parser<T, A>,
                                mid: Parser<T, B>,
                                right: Parser<T, C>,
                                crossinline body: (A, B, C) -> D): Parser<T, D> =
    left.flatMap { l -> mid.flatMap { m -> right.map { r -> body(l, m, r) } } }
