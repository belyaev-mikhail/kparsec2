package ru.spbstu.parsers.combinators

import ru.spbstu.*

fun eof(): Parser<Any?, Unit> = Parser {
    if (!it.hasNext()) it.unitSuccess()
    else it.failure("<EOF>", it.current)
}

fun <R> success(value: R): Parser<Any?, R> = Parser { ParseSuccess(it, value) }
fun <R> failure(expected: Any?, actual: Any?): Parser<Any?, R> = Parser { it.failure(expected, actual) }

fun cursor(): Parser<Any?, Location<*>> = Parser { ParseSuccess(it, it.location) }
