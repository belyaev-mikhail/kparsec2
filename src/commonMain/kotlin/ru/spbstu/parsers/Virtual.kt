package ru.spbstu.parsers

import ru.spbstu.*

fun <T> eof(): Parser<T, Unit> = Parser {
    if (!it.hasNext()) it.unitSuccess()
    else it.failure("<EOF>", it.current.toString())
}

fun <T, R> success(value: R): Parser<T, R> = Parser { ParseSuccess(it, value) }
fun <T, R> failure(expected: String, actual: String): Parser<T, R> = Parser { it.failure(expected, actual) }

fun <T> cursor(): Parser<T, Location<*>> = Parser { ParseSuccess(it, it.location) }
