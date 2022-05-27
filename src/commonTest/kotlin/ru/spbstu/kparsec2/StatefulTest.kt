package ru.spbstu.kparsec2

import ru.spbstu.kparsec2.parsers.combinators.*
import ru.spbstu.kparsec2.parsers.dsl.or
import ru.spbstu.kparsec2.parsers.dsl.plus
import ru.spbstu.kparsec2.parsers.dsl.unaryMinus
import ru.spbstu.kparsec2.parsers.library.Spaces.newline
import ru.spbstu.kparsec2.parsers.library.Spaces.spaces
import ru.spbstu.kparsec2.parsers.packrat.packrat
import ru.spbstu.kparsec2.parsers.stateful.parserState
import ru.spbstu.kparsec2.parsers.stateful.stateGet
import ru.spbstu.kparsec2.parsers.stateful.stateModifyIfSet
import ru.spbstu.kparsec2.parsers.stateful.stateSet
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

    data class Tree(val value: String, val children: List<Tree> = listOf())

    object TreeGrammar {
        data class Indent(val amount: Int = 0)

        val indent: Parser<Char, Indent> = packrat(parserDo {
            newline()
            val ws = spaces()
            var indent: Indent by parserState<Char, Indent>()
            val res = Indent(ws.length - indent.amount)
            indent = Indent(ws.length)
            res
        }) named "indent"

        val plusIndent = indent.filter("indent>0") { it.amount > 0 }
        val minusIndent = eof or indent.filter("indent<0") { it.amount < 0 }
        val level = indent.filter("indent==0") { it.amount == 0 }

        val ident = manyOneAsString(oneOf('a'..'z'))

        val tree: Parser<Char, Tree> = zipWith(ident, optional(lazyParser { body })) { v, ch ->
            Tree(v.toString(), ch.orEmpty())
        }
        val body = -plusIndent + separatedBy(tree, -optional(-level)) + -minusIndent

        val whole = packrat(stateSet(Indent(0)) + tree + eof)
    }

    @Test
    fun indentation() {
        println(TreeGrammar.whole)
        println(TreeGrammar.body)
        println(TreeGrammar.body)

        val pp = TreeGrammar.whole(
            """
            a
               b
                 d
               c
        """.trimIndent()
        )

        assertSuccess(pp)
        assertEquals(
            Tree("a", listOf(
                Tree("b", listOf(
                    Tree("d")
                )),
                Tree("c")
            )),
            pp.result
        )
    }
}
