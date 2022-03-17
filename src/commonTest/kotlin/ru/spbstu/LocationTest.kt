package ru.spbstu.ru.spbstu

import ru.spbstu.CharLocation
import ru.spbstu.Location
import ru.spbstu.OffsetLocation
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationTest {
    val text = """
    If you can keep your head when all about you   
    Are losing theirs and blaming it on you,   
    If you can trust yourself when all men doubt you,
    But make allowance for their doubting too;   
    If you can wait and not be tired by waiting,
    Or being lied about, don’t deal in lies,
    Or being hated, don’t give way to hating,
    And yet don’t look too good, nor talk too wise.
    """.trimIndent()

    fun finalLocus(loc: Location<Char>): Location<Char> {
        var acc = loc
        for (ch in text) {
            println(ch)
            println(acc)
            if ((ch == '\n')) {
                println("!")
            }
            acc = acc(ch)
        }
        return acc
    }

    @Test
    fun basic() {
        val lines = text.lines()

        assertEquals(
            CharLocation(lines.size, lines.last().length),
            finalLocus(CharLocation())
        )
        assertEquals(
            OffsetLocation(text.length),
            finalLocus(OffsetLocation())
        )
    }
}