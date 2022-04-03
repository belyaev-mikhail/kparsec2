package ru.spbstu

import ru.spbstu.parsers.combinators.flatMap
import ru.spbstu.parsers.combinators.map
import ru.spbstu.parsers.combinators.parserDo
import ru.spbstu.parsers.combinators.sequence
import ru.spbstu.parsers.dsl.plus
import ru.spbstu.parsers.sequence
import ru.spbstu.parsers.token

import kotlin.test.Test
import kotlin.test.assertEquals

private fun String.toCharList() = toCharArray().asList()

class RegressionTest {
    @Test
    fun tokenSequenceRegression0() {
        val parsers: List<Parser<Char, List<Char>>> = listOf(
            sequence("abcd").map(String::toCharList),
            sequence('a', 'b', 'c', 'd'),
            sequence(token('a'), token('b'), token('c'), token('d')),
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

}