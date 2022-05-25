package ru.spbstu

import ru.spbstu.parsers.combinators.*
import ru.spbstu.parsers.dsl.or
import ru.spbstu.parsers.dsl.plus
import ru.spbstu.parsers.dsl.unaryMinus
import ru.spbstu.parsers.library.Spaces.newline
import ru.spbstu.parsers.library.Spaces.spaces
import ru.spbstu.parsers.stateful.stateGet
import ru.spbstu.parsers.stateful.stateModifyIfSet
import ru.spbstu.parsers.stateful.stateSet
import ru.spbstu.ru.spbstu.SimpleCompleteParserTest
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

    data class Tree(val value: String, val children: List<Tree>)

    object TreeGrammar {
        data class Indent(val amount: Int = 0)
        val indent = newline + spaces.flatMap { ws ->
            stateGet<Indent>().flatMap { old ->
                success(Indent(ws.size - old.amount)) + stateSet(Indent(ws.size))
            }
        }

        val plusIndent = indent.filter { it.amount > 0 }
        val minusIndent = indent.filter { it.amount < 0 }

        val ident = manyOneAsString(oneOf('a'..'z'))

        val tree: Parser<Char, Tree> = zipWith(ident, lazyParser { body }) { v, ch ->
            Tree(v.toString(), ch)
        }
        val body = -plusIndent + many(tree) + -minusIndent

        val whole = stateSet(Indent(0)) + tree
    }

    @Test
    fun indentation() {
        println(TreeGrammar.whole("""
            a
               b
               c
                 d
               e
        """.trimIndent()))
    }
}
