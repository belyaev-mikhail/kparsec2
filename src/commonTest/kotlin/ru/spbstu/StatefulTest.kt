package ru.spbstu

import ru.spbstu.parsers.combinators.many
import ru.spbstu.parsers.combinators.token
import ru.spbstu.parsers.dsl.or
import ru.spbstu.parsers.dsl.plus
import ru.spbstu.parsers.dsl.unaryMinus
import ru.spbstu.parsers.stateful.stateGet
import ru.spbstu.parsers.stateful.stateModifyIfSet
import ru.spbstu.wheels.getOrElse
import kotlin.test.Test
import kotlin.test.assertEquals

class StatefulTest {
    val parenInput = stringInput("((())(((()((((")

    val LPAREN = -token('(') + stateModifyIfSet<Int> { it.getOrElse { 0 } + 1 }
    val RPAREN = -token(')') + stateModifyIfSet<Int> { it.getOrElse { 0 } - 1 }

    val total = many(LPAREN or RPAREN) + stateGet<Int>()

    @Test
    fun simple() {
        assertEquals(8, total(parenInput).resultOrThrow)
    }

    @Test
    fun twoState() {
        assertEquals(8, total(parenInput).resultOrThrow)
    }
}
