package ru.spbstu.parsers.combinators

import ru.spbstu.*

fun <T, A> oneOf(parsers: Iterator<Parser<T, A>>): Parser<T, A> = Parser {
    for (parser in parsers) {
        when(val res = parser(it)) {
            is ParseSuccess -> return@Parser res
            else -> continue
        }
    }
    it.failure("<one of ${parsers}>", it.current.toString())
}
fun <T, A> oneOf(parsers: Iterable<Parser<T, A>>): Parser<T, A> = oneOf(parsers.iterator())
fun <T, A> oneOf(vararg parsers: Parser<T, A>): Parser<T, A> = oneOf(parsers.iterator())

fun <T, A> sequence(iterator: Iterator<Parser<T, A>>): Parser<T, List<A>> = Parser {
    val result = mutableListOf<A>()
    var self = it
    for (element in iterator) {
        when (val current = element(self)) {
            is ParseSuccess -> {
                result += current.result
                self = current.rest
            }
            is NoSuccess -> return@Parser current
        }
    }
    ParseSuccess(self, result)
}
fun <T, A> sequence(parsers: Iterable<Parser<T, A>>): Parser<T, List<A>> = sequence(parsers.iterator())
fun <T, A> sequence(vararg parsers: Parser<T, A>): Parser<T, List<A>> = sequence(parsers.iterator())

fun <T, A, M: MutableCollection<A>> manyTo(collection: M,
                                           parser: Parser<T, A>): Parser<T, M> = Parser {
    var self = it
    do {
        when (val current = parser(self)) {
            is ParseSuccess -> {
                collection += current.result
                self = current.rest
            }
            is Failure -> break
            is Error -> return@Parser current
        }
    } while (self.hasNext())
    ParseSuccess(self, collection)
}

fun <T, A> many(parser: Parser<T, A>): Parser<T, List<A>> = manyTo(mutableListOf(), parser)

fun <T, A> manyOne(parser: Parser<T, A>): Parser<T, List<A>> = parser.flatMap { first ->
    val acc = mutableListOf(first)
    manyTo(acc, parser)
}
