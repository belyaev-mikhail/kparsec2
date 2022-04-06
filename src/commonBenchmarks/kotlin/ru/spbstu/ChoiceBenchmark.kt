package ru.spbstu

import kotlinx.benchmark.*
import ru.spbstu.parsers.combinators.oneOf
import ru.spbstu.parsers.combinators.token
import ru.spbstu.parsers.dsl.or

private const val ONE_OF_STRING = "oneOfString"
private const val ONE_OF_VARARG = "oneOfVararg"
private const val PREDICATE = "predicate"
private const val ONE_OF_PARSERS = "oneOfParsers"
private const val OR = "or"

private const val SUCCESS_ON_FIRST = "successOnFirst"
private const val SUCCESS_ON_LAST = "successOnLast"
private const val SUCCESS_ON_MID = "successOnMid"
private const val FAILURE = "failure"
private const val EMPTY = "empty"

@State(Scope.Benchmark)
open class ChoiceBenchmark {
    @Param(SUCCESS_ON_FIRST, SUCCESS_ON_MID, SUCCESS_ON_LAST, FAILURE, EMPTY)
    lateinit var inputKind: String

    @Param(ONE_OF_STRING, ONE_OF_VARARG, PREDICATE, ONE_OF_PARSERS, OR)
    lateinit var parserKind: String

    lateinit var parser: Parser<Char, *>
    lateinit var input: Input<Char>

    @Setup
    fun createAll() {
        parser = when(parserKind) {
            ONE_OF_STRING -> oneOf("abcde")
            ONE_OF_VARARG -> oneOf('a', 'b', 'c', 'd', 'e')
            PREDICATE -> {
                val set = "abcde".toSet()
                token("[abcde]") { it in set }
            }
            ONE_OF_PARSERS -> oneOf(token('a'), token('b'), token('c'), token('d'), token('e'))
            OR -> token('a') or token('b') or token('c') or token('d') or token('e')
            else -> error("")
        }

        val inputData = when(inputKind) {
            SUCCESS_ON_FIRST -> "abcd"
            SUCCESS_ON_MID -> "cdef"
            SUCCESS_ON_LAST -> "efgh"
            FAILURE -> "xyz"
            EMPTY -> ""
            else -> ""
        }

        input = stringInput(inputData, CharLocationType.OFFSET)

    }

    @Benchmark
    fun run(bh: Blackhole) {
        repeat(1000) {
            bh.consume(parser(input).toString())
        }
    }
}