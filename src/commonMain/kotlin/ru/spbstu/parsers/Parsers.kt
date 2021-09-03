package ru.spbstu.parsers

import ru.spbstu.*

fun <T> token(value: T): Parser<T, T> = Parser {
    when(val current = it.current) {
        value -> ParseSuccess(it.advance(), current)
        else -> it.failure("$value", "$current")
    }
}

inline fun <T> token(expectedString: String = "<predicate>", crossinline predicate: (T) -> Boolean): Parser<T, T> = Parser {
    val current = it.current
    when {
        predicate(current) -> ParseSuccess(it.advance(), current)
        else -> it.failure(expectedString, "$current")
    }
}

fun <T> choice(tokens: Set<T>): Parser<T, T> = run {
    token("<one of $tokens>") { it in tokens }
}

fun <T> choice(vararg tokens: T): Parser<T, T> = choice(tokens.toSet())
fun <T> choice(tokens: Iterator<T>): Parser<T, T> = choice(tokens.asSequence().toSet())
fun <T> choice(tokens: Iterable<T>): Parser<T, T> = choice(tokens.asSequence().toSet())

fun <T> sequence(iterator: Iterator<T>, expectedString: String = "<predicate>"): Parser<T, List<T>> = Parser {
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
                return@Parser it.failure(expectedString, "$result")
            }
        }
    }
    ParseSuccess(self, result)
}

fun <T> sequence(vararg tokens: T): Parser<T, List<T>> = sequence(tokens.iterator(), tokens.joinToString())
fun <T> sequence(tokens: Iterable<T>): Parser<T, List<T>> = sequence(tokens.iterator(), tokens.joinToString())

