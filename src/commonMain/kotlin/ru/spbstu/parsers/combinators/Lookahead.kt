package ru.spbstu.parsers.combinators

import ru.spbstu.*

fun <T, R> peek(parser: Parser<T, R>): Parser<T, R> = Parser {
    when(val res = parser(it)) {
        is ParseSuccess -> res.copy(rest = it)
        else -> res
    }
}

fun <T> not(parser: Parser<T, Any?>): Parser<T, Unit> = Parser {
    when(val res = parser(it)) {
        is ParseSuccess -> it.failure("<not $parser>", it.current.toString())
        is ParseFailure -> it.unitSuccess()
        is ParseError -> res
    }
}
