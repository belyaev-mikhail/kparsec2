package ru.spbstu

import ru.spbstu.parsers.liftParser
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

class FileSourceTest {
    @Test
    fun simple() {
        val JSON = liftParser(JsonParserTest.JsonGrammar.expr, JsonParserTest.JsonTokens.token, JsonParserTest.JsonTokens.space)
        val file = Paths.get(javaClass.classLoader.getResource("Tryout.json")!!.toURI())
        val result = JSON(fileInput(file))
        assertSuccess(result)
        val r: JsonParserTest.JsonExpr = result.result
    }
}