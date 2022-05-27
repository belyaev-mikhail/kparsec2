package ru.spbstu.kparsec2

import ru.spbstu.kparsec2.parsers.combinators.*
import ru.spbstu.kparsec2.parsers.dsl.or
import ru.spbstu.kparsec2.parsers.dsl.plus

import kotlin.test.Test

private fun String.toCharList() = toCharArray().asList()

class RegressionTest {
    @Test
    fun tokenSequenceRegression0() {
        val parsers: List<Parser<Char, List<Char>>> = listOf(
            ru.spbstu.kparsec2.parsers.combinators.sequence("abcd").map(String::toCharList),
            sequence('a', 'b', 'c', 'd'),
            ru.spbstu.kparsec2.parsers.combinators.sequence(token('a'), token('b'), token('c'), token('d')),
            token('a') + token('b') + token('c') + token('d'),
            token('a').flatMap { a ->
                token('b').flatMap { b ->
                    token('c').flatMap { c ->
                        token('d').map { d -> listOf(a, b, c, d) }
                    }
                }
            },
            parserDo {
                val a = parse(token('a'))
                val b = parse(token('b'))
                val c = parse(token('c'))
                val d = parse(token('d'))
                listOf(a, b, c, d)
            }
        )

        fun assertEqualBehaviour(input: String) {
            val results = parsers.map { it(input) }
            val first = results.first()
            for (result in results) {
                assertEqualResults(first, result, deepCompareErrors = false)
            }
        }

        assertEqualBehaviour("abcd")
        assertEqualBehaviour("abc*")
        assertEqualBehaviour("feqr")
        assertEqualBehaviour("")
    }

    @Test
    fun tokenOneOfRegression() {
        val parsers: List<Parser<Char, Char>> = listOf(
            oneOf("abcde"),
            oneOf('a', 'b', 'c', 'd', 'e'),
            oneOf(token('a'), token('b'), token('c'), token('d'), token('e')),
            token('a') or token('b') or token('c') or token('d') or token('e')
        )

        fun assertEqualBehaviour(input: String) {
            val results = parsers.map { it(input) }
            val first = results.first()
            for (result in results) {
                assertEqualResults(first, result, deepCompareErrors = false)
            }
        }

        assertEqualBehaviour("abc")
        assertEqualBehaviour("cde")
        assertEqualBehaviour("efg")
        assertEqualBehaviour("xyz")
    }

}