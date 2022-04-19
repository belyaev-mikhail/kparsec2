package ru.spbstu.ru.spbstu

import ru.spbstu.JsonParserTest
import ru.spbstu.assertSuccess
import ru.spbstu.fileInput
import ru.spbstu.parsers.liftParser
import ru.spbstu.stringInput
import java.io.StringReader
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals

class FileSourceTest {
    @Test
    fun simple() {
        val JSON = liftParser(
            JsonParserTest.JsonGrammar.expr,
            JsonParserTest.JsonTokens.token,
            JsonParserTest.JsonTokens.space
        )
        val file = Paths.get(javaClass.classLoader.getResource("Tryout.json")!!.toURI())
        val result = JSON(fileInput(file, bufferSize = 256))
        assertSuccess(result)
        val r: JsonParserTest.JsonExpr = result.result
        val result2 = JSON(stringInput("$r"))
        assertSuccess(result2)
        assertEquals(result.result, result2.result)

        val result3 = JSON(stringInput(file.readText()))
        assertSuccess(result3)
        assertEquals(result3.result, result.result)
    }
}