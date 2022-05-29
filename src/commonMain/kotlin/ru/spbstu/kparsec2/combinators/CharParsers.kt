package ru.spbstu.kparsec2.parsers.combinators

import ru.spbstu.kparsec2.*
import ru.spbstu.kparsec2.util.quoteString

fun oneOf(tokens: String): Parser<Char, Char> = when(tokens.length) {
    1 -> token(tokens.single())
    else -> token("<one of ${quoteString(tokens)}>") { it in tokens }
}

fun sequence(tokens: String): Parser<Char, String> = when(tokens.length) {
    0 -> success("")
    1 -> token(tokens.single()).map { it.toString() }
    else -> namedParser(quoteString(tokens)) {
        val sourceTokens = it.takeString(tokens.length)
        if (sourceTokens != tokens) it.failure(name, actual = sourceTokens)
        else ParseSuccess(it.drop(tokens.length), sourceTokens)
    }
}

inline fun manyAsString(expected: String, crossinline pred: (Char) -> Boolean): Parser<Char, CharSequence> =
    manyFold({ StringBuilder() }, token(expected, pred)) { append(it) } named expected

fun <T> manyAsString(token: Parser<T, Char>): Parser<T, CharSequence> =
    manyFold({ StringBuilder() }, token) { append(it) }

inline fun manyOneAsString(expected: String, crossinline pred: (Char) -> Boolean): Parser<Char, CharSequence> =
    manyOneFold({ StringBuilder() }, token(expected, pred)) { append(it) } named expected

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
