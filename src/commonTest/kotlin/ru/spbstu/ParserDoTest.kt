package ru.spbstu

import ru.spbstu.parsers.combinators.*
import ru.spbstu.parsers.dsl.or
import ru.spbstu.parsers.dsl.plus
import ru.spbstu.parsers.dsl.unaryMinus
import ru.spbstu.parsers.library.Numbers
import ru.spbstu.parsers.library.Spaces
import ru.spbstu.parsers.token
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserDoTest {

    val number = Numbers.decimalNumber
    val plus = token('+').map { _ -> { a: Int, b: Int -> a + b } }
    val minus = token('-').map { _ -> { a: Int, b: Int -> a - b} }
    val star = token('*').map { _ -> { a: Int, b: Int -> a * b} }
    val slash = token('/').map { _ -> { a: Int, b: Int -> a / b} }
    val lparen = -token('(')
    val rparen = -token(')')
    val spaces = -Spaces.spaces

    val simpleExpr: Parser<Char, Int> = lazyParser {
        number or parserDo {
            parse(lparen)
            parse(spaces)
            val res = parse(expr)
            parse(spaces)
            parse(rparen)
            res
        }
    }
    val prod = parserDo<Char, Int> {
        var lhv = parse(simpleExpr)
        while (true) {
            parse(spaces)
            val op = tryParse(star or slash) ?: break
            parse(spaces)
            val rhv = parse(simpleExpr)
            lhv = op(lhv, rhv)
        }
        lhv
    }
    val sum = parserDo<Char, Int> {
        var lhv = parse(prod)
        while (true) {
            parse(spaces)
            val op = tryParse(plus or minus) ?: break
            parse(spaces)
            val rhv = parse(prod)
            lhv = op(lhv, rhv)
        }
        lhv
    }
    val expr: Parser<Char, Int> = parserDo {
        parse(spaces)
        val res = parse(sum)
        parse(spaces)
        res
    }

    @Test
    fun basic() {
        val r1 = number("20")
        assertEquals(20, r1.resultOrThrow)
        val r2 = expr("(2+3)*4/2+5*2")
        assertEquals(20, r2.resultOrThrow)
        val r3 = expr(" ( 2 + 3) *4/ 2+ 5* 2")
        assertEquals(20, r3.resultOrThrow)
    }

}