package ru.spbstu.parsers

import ru.spbstu.ParseSuccess
import ru.spbstu.Parser
import ru.spbstu.failure
import ru.spbstu.parsers.combinators.map
import ru.spbstu.parsers.combinators.namedParser
import ru.spbstu.takeString

fun oneOf(tokens: String): Parser<Char, Char> = when(tokens.length) {
    1 -> token(tokens.single())
    else -> token("<one of \"$tokens\">") { it in tokens }
}

fun sequence(tokens: String): Parser<Char, String> = when(tokens.length) {
    0 -> success("")
    1 -> token(tokens.single()).map { it.toString() }
    else -> namedParser("\"$tokens\"") {
        val sourceTokens = it.takeString(tokens.length)
        if (sourceTokens != tokens) it.failure(tokens, sourceTokens)
        ParseSuccess(it.drop(tokens.length), sourceTokens)
    }
}
