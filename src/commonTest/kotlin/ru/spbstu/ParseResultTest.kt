package ru.spbstu

import ru.spbstu.parsers.*
import kotlin.test.*

class ParseResultTest {
    @Test
    fun basic() {
        val success = success<Char, String>("Hello").invoke("")
        runIsolated {
            assertSuccess(success)
            assertEquals("Hello", success.result)
        }
        assertEquals("Hello", success.resultOrThrow)
        assertEquals(success, success.successOrThrow)

        val failure = failure<Char, String>("a", "b").invoke("")
        runIsolated {
            assertTrue(failure !is ParseSuccess)
            assertTrue(failure is NoSuccess)
            assertTrue(failure is ParseFailure)
        }

        val ex = assertFailsWith<ParseException> {
            failure.resultOrThrow
        }

        assertEquals(failure, ex.result)

        assertFailsWith<ParseException> {
            failure.restOrThrow
        }
    }

    @Test
    fun combinators() {
        val success = success<Char, String>("Hello").invoke("")

        val mapped = success.map { it + " World" }
        assertSuccess(mapped)
        assertEquals("Hello World", mapped.resultOrThrow)

        assertEquals("Hello World", success.flatMap { success<Char, String>(it + " World").invoke("") }.resultOrThrow)
        assertEquals(success, success.flatMap { success })

        val failure = failure<Char, String>("a", "b").invoke("")

        val fmapped = failure.map { it + " World" }
        assertTrue(fmapped is ParseFailure)
        assertEquals(failure, fmapped)

        assertEquals(failure, failure.flatMap { success })
        assertEquals(failure, success.flatMap { failure })
        assertEquals(failure, failure.flatMap { failure })
    }
}
