package ru.spbstu.parsers.combinators

import ru.spbstu.*

val eof: Parser<Any?, Unit> = namedParser("<EOF>") {
    if (!it.hasNext()) it.unitSuccess()
    else it.failure()
}

fun <R> success(value: R): Parser<Any?, R> = namedParser("<success($value)>")  { ParseSuccess(it, value) }
fun <R> failure(expected: Any?, actual: Any?): Parser<Any?, R> =
    namedParser("<failure($expected, $actual)>") { it.failure(expected, actual) }

val cursor: Parser<Any?, Location<*>> = namedParser("<cursor>") { ParseSuccess(it, it.location) }
