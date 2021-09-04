package ru.spbstu

import ru.spbstu.parsers.sequence
import ru.spbstu.parsers.token
import kotlin.test.Test
import kotlin.test.assertEquals

class Smokey {
    @Test
    fun smokeTest() {
        val inp = stringInput("<<<>>>")
        assertEquals('<', token('<')(inp).resultOrThrow)
        assertEquals(listOf('<', '<', '<'), sequence('<', '<', '<')(inp).resultOrThrow)
    }

    @Test
    fun lispyLisp() {

    }

}