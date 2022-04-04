package ru.spbstu.parsers.combinators

import ru.spbstu.*

fun <T, R> peek(parser: Parser<T, R>): Parser<T, R> = namedParser("$parser") {
    when(val res = parser(it)) {
        is ParseSuccess -> res.copy(rest = it)
        else -> res
    }
}

fun <T> not(parser: Parser<T, Any?>): Parser<T, Unit> = namedParser("!($parser)") {
    when(val res = parser(it)) {
        is ParseSuccess -> it.failure()
        is ParseFailure -> it.unitSuccess()
        is ParseError -> res
    }
}
