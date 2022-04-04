package ru.spbstu.parsers.combinators

import ru.spbstu.*

fun oneOf(tokens: String): Parser<Char, Char> = when(tokens.length) {
    1 -> token(tokens.single())
    else -> token("<one of \"$tokens\">") { it in tokens }
}

fun sequence(tokens: String): Parser<Char, String> = when(tokens.length) {
    0 -> success("")
    1 -> token(tokens.single()).map { it.toString() }
    else -> namedParser("\"$tokens\"") {
        val sourceTokens = it.takeString(tokens.length)
        if (sourceTokens != tokens) it.failure(name, actual = sourceTokens)
        else ParseSuccess(it.drop(tokens.length), sourceTokens)
    }
}

inline fun manyAsString(expected: String, crossinline pred: (Char) -> Boolean): Parser<Char, String> =
    manyTokens(expected, pred).map { it.joinToString(separator = "") }

fun <T> manyAsString(token: Parser<T, Char>): Parser<T, CharSequence> =
    manyFold({ StringBuilder() }, token) { append(it) }

fun <T> manyOneAsString(token: Parser<T, Char>): Parser<T, CharSequence> =
    manyOneFold({ StringBuilder() }, token) { append(it) }

@ExperimentalStdlibApi
fun regex(re: Regex): Parser<Char, MatchResult> = namedParser("regex(${re.pattern})") body@{
    val res = re.matchAt(it.asCharSequence(), 0)
        ?: return@body it.failure("regex(${re.pattern})")
    val shifted = it.drop(res.range.last + 1)
    ParseSuccess(shifted, res)
}

@ExperimentalStdlibApi
fun regex(re: String): Parser<Char, MatchResult> = regex(Regex(re))
