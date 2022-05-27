package ru.spbstu.kparsec2

import kotlinx.benchmark.*
import ru.spbstu.kparsec2.parsers.combinators.flatMap
import ru.spbstu.kparsec2.parsers.combinators.map
import ru.spbstu.kparsec2.parsers.combinators.parserDo
import ru.spbstu.kparsec2.parsers.combinators.sequence
import ru.spbstu.kparsec2.parsers.dsl.plus
import ru.spbstu.kparsec2.parsers.combinators.token

private const val STRING = "string"
private const val VARARG = "vararg"
private const val SEQUENCE = "sequence"
private const val PLUS = "plus"
private const val FLAT_MAP = "flatMap"
private const val PARSER_DO = "parserDo"

@State(Scope.Benchmark)
open class SequenceBenchmark {
    @Param(STRING, VARARG, SEQUENCE, PLUS, FLAT_MAP, PARSER_DO)
    lateinit var creator: String

    lateinit var parser: Parser<Char, *>
    lateinit var goodInput: Input<Char>
    lateinit var fairInput: Input<Char>
    lateinit var badInput: Input<Char>

    @Setup
    fun createAll() {

        parser = when(creator) {
            STRING -> sequence("abcd")
            VARARG -> sequence('a', 'b', 'c', 'd')
            SEQUENCE -> sequence(token('a'), token('b'), token('c'), token('d'))
            PLUS -> token('a') + token('b') + token('c') + token('d')
            FLAT_MAP ->
                token('a').flatMap { a ->
                    token('b').flatMap { b ->
                        token('c').flatMap { c ->
                            token('d').map { d -> listOf(a, b, c, d) }
                        }
                    }
                }
            PARSER_DO -> parserDo {
                val a = parse(token('a'))
                val b = parse(token('b'))
                val c = parse(token('c'))
                val d = parse(token('d'))
                listOf(a, b, c, d)
            }
            else -> throw IllegalStateException()
        }
        goodInput = stringInput("abcd", CharLocationType.OFFSET)
        fairInput = stringInput("abd*", CharLocationType.OFFSET)
        badInput = stringInput("pliqdc", CharLocationType.OFFSET)
    }

    @Benchmark
    fun success(bh: Blackhole) {
        repeat(1000) {
            bh.consume(parser.invoke(goodInput).resultOrThrow)
        }
    }

    @Benchmark
    fun partialFail(bh: Blackhole) {
        repeat(1000) {
            bh.consume(parser.invoke(fairInput))
        }
    }

    @Benchmark
    fun totalFail(bh: Blackhole) {
        repeat(1000) {
            bh.consume(parser.invoke(badInput))
        }
    }
}
