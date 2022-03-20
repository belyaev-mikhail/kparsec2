package ru.spbstu.parsers

import ru.spbstu.*
import ru.spbstu.parsers.combinators.filter
import ru.spbstu.parsers.combinators.map
import ru.spbstu.parsers.combinators.namedParser
import ru.spbstu.ParseError as ParseError

fun <T> any(): Parser<T, T> = namedParser("<any>") {
    if (!it.hasNext()) return@namedParser it.failure("<any>", "<end of input>")
    val current = it.current
    ParseSuccess(it.advance(), current)
}

inline fun <T> token(expectedString: String = "<predicate>", crossinline predicate: (T) -> Boolean): Parser<T, T> =
    namedParser(expectedString) {
        if (!it.hasNext()) return@namedParser it.failure(expectedString, "<end of input>")
        val current = it.current
        when {
            predicate(current) -> ParseSuccess(it.advance(), current)
            else -> it.failure(expectedString, "$current")
        }
    }

fun <T> token(value: T): Parser<T, T> = token("$value") { it == value }

inline fun <T, reified S: T> token(expectedString: String = "${S::class}"): Parser<T, S> =
    @Suppress("UNCHECKED_CAST")
    (token<T>(expectedString) { it is S } as Parser<T, S>)

fun <T> oneOf(tokens: Set<T>): Parser<T, T> = when(tokens.size) {
    1 -> token(tokens.single())
    else -> token("<one of $tokens>") { it in tokens }
}

fun <T> oneOf(vararg tokens: T): Parser<T, T> = when(tokens.size) {
    1 -> token(tokens.single())
    else -> oneOf(tokens.toSet())
}
fun <T> oneOf(tokens: Iterable<T>): Parser<T, T> = oneOf(tokens.toSet())

fun <T> sequence(iterator: Iterable<T>, expectedString: String = "<predicate>"): Parser<T, List<T>> =
    namedParser(expectedString) {
        val result = mutableListOf<T>()
        var self = it
        for (element in iterator) {
            when (val current = self.current) {
                element -> {
                    result += current
                    self = self.advance()
                }
                else -> {
                    result += current
                    return@namedParser it.failure(expectedString, "$result")
                }
            }
        }
        ParseSuccess(self, result)
    }

fun <T> sequence(vararg tokens: T): Parser<T, List<T>> = when(tokens.size) {
    1 -> token(tokens.single()).map { listOf(it) }
    else -> sequence(tokens.asList())
}
fun <T> sequence(tokens: Iterable<T>): Parser<T, List<T>> = when(tokens) {
    is Collection -> sequence(tokens as Collection<T>)
    else -> sequence(tokens, expectedString = tokens.joinToString())
}
fun <T> sequence(tokens: Collection<T>): Parser<T, List<T>> = when(tokens.size) {
    1 -> token(tokens.single()).map { listOf(it) }
    else -> namedParser(tokens.joinToString(" ")) {
        val sourceTokens = mutableListOf<T>()
        it.takeTo(sourceTokens, tokens.size)
        if (sourceTokens != tokens) it.failure("$tokens", "$sourceTokens")
        ParseSuccess(it.drop(tokens.size), sourceTokens)
    }
}

inline fun <T> manyOneTokens(
                    expectedString: String = "<predicate>",
                    crossinline pred: (T) -> Boolean): Parser<T, List<T>> =
    manyTokens(expectedString, pred).filter(expectedString) { it.size > 1 }

inline fun <T> manyTokens(
    expectedString: String = "<predicate>",
    crossinline pred: (T) -> Boolean): Parser<T, List<T>> =
    namedParser(expectedString) {
        val result = mutableListOf<T>()
        var self = it
        while (self.hasNext()) {
            val current = self.current
            when {
                pred(current) -> {
                    result += current
                    self = self.advance()
                }
                else -> break
            }
        }
        ParseSuccess(self, result as List<T>)
    }

inline fun <T, R> choice(crossinline body: (T) -> Parser<T, R>): Parser<T, R> = Parser {
    body(it.current).invoke(it.advance())
}

fun <T> not(tokenParser: Parser<T, T>): Parser<T, T> = Parser {
    when(val result = tokenParser(it)) {
        is ParseSuccess -> it.failure("!${result.result}", "${result.result}")
        is ParseFailure -> ParseSuccess(it.advance(), it.current)
        is ParseError -> result
    }
}
