package ru.spbstu.kparsec2

import ru.spbstu.kparsec2.parsers.combinators.namedParser

@Suppress("UNCHECKED_CAST")
private fun <T, T1> unliftInput(input: Input<T>): Input<T1> = when(input) {
    is ParsedInput<*, *> -> input.parsedSource.input as Input<T1>
    else -> error("input is not lifted")
}

fun <T0, T1, R> liftParser(
    parser: Parser<T1, R>,
    tokenParser: Parser<T0, T1>,
    ignore: Parser<T0, Unit>? = null
): Parser<T0, R> = namedParser({ "$parser" }) {
    val liftedInput = parsedInput(it, tokenParser, ignore)
    val result = parser(liftedInput)
    when (result) {
        is NoSuccess -> result
        is ParseSuccess -> ParseSuccess(unliftInput(result.rest), result.result)
    }
}
