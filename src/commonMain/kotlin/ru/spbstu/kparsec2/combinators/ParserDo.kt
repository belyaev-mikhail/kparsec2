package ru.spbstu.kparsec2.parsers.combinators

import ru.spbstu.*
import ru.spbstu.kparsec2.*
import ru.spbstu.wheels.NoStackThrowable
import kotlin.experimental.ExperimentalTypeInference

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
inline fun <T, R> parserDo(@BuilderInference crossinline body: DoScope<T>.() -> R): Parser<T, R> = Parser { input ->
    val scope = DoScope(input)
    try {
        val result = scope.body()
        ParseSuccess(result = result, rest = scope.input)
    } catch (ex: ParserDoException) {
        ex.result
    }
}

