package ru.spbstu.kparsec2

import ru.spbstu.kparsec2.parsers.combinators.regex
import ru.spbstu.kparsec2.invoke
import ru.spbstu.kparsec2.parsers.combinators.zipWith
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class RegexTest {
    @Test
    fun testSimple() {
        val anyRe = regex(".*")
        assertEquals(
            "abcdefg()",
            anyRe("abcdefg()").resultOrThrow.value
        )

        val re = regex(Regex("\\d+"))

        assertEquals(
            "1234",
            re("1234asdas").resultOrThrow.value
        )

        val reCom = zipWith(regex("\\d+"), regex(".*")) { a, b ->
             b.value.repeat(a.value.toInt())
        }

        assertEquals(
            "abababab",
            reCom("4ab").resultOrThrow
        )

    }
}