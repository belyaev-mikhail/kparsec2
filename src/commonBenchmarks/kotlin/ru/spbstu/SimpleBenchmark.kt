package ru.spbstu

import kotlinx.benchmark.*
import ru.spbstu.parsers.combinators.flatMap
import ru.spbstu.parsers.combinators.map
import ru.spbstu.parsers.combinators.parserDo
import ru.spbstu.parsers.combinators.sequence
import ru.spbstu.parsers.dsl.plus
import ru.spbstu.parsers.combinators.token

@State(Scope.Benchmark)
open class SimpleBenchmark {
    @Param("string", "vararg", "sequence", "plus", "flatMap", "parserDo")
    var creator: String = ""

    lateinit var parser: Parser<Char, *>
    lateinit var goodInput: Input<Char>
    lateinit var fairInput: Input<Char>
    lateinit var badInput: Input<Char>

    @Setup
    fun createAll() {

        parser = when(creator) {
            "string" -> sequence("abcd")
            "vararg" -> sequence('a', 'b', 'c', 'd')
            "sequence" -> sequence(token('a'), token('b'), token('c'), token('d'))
            "plus" -> token('a') + token('b') + token('c') + token('d')
            "flatMap" ->
                token('a').flatMap { a ->
                    token('b').flatMap { b ->
                        token('c').flatMap { c ->
                            token('d').map { d -> listOf(a, b, c, d) }
                        }
                    }
                }
            "parserDo" -> parserDo {
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
