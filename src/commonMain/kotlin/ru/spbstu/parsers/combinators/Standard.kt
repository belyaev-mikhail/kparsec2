package ru.spbstu.parsers.combinators

import ru.spbstu.*
import kotlin.jvm.JvmName

fun <T, A> oneOf(parsers: Iterable<Parser<T, A>>): Parser<T, A> = Parser {
    for (parser in parsers) {
        when (val res = parser(it)) {
            is ParseSuccess -> return@Parser res
            else -> continue
        }
    }
    it.failure("<one of ${parsers}>", it.currentOrNull?.toString() ?: "<end of input>")
}

fun <T, A> oneOf(vararg parsers: Parser<T, A>): Parser<T, A> = oneOf(parsers.asList())

fun <T, A> sequence(parsers: Iterable<Parser<T, A>>): Parser<T, List<A>> = Parser {
    val result = mutableListOf<A>()
    var self = it
    for (element in parsers) {
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

fun <T, A> sequence(vararg parsers: Parser<T, A>): Parser<T, List<A>> = sequence(parsers.asList())

inline fun <T, A, M : MutableCollection<A>> manyTo(
    crossinline collectionFactory: () -> M,
    parser: Parser<T, A>
): Parser<T, M> = Parser {
    val collection = collectionFactory()
    var self = it
    do {
        when (val current = parser(self)) {
            is ParseSuccess -> {
                collection += current.result
                if (current.rest === self) break // do not attempt to repeat non-consuming parsers
                self = current.rest
            }
            is ParseFailure -> break
            is ParseError -> return@Parser current
        }
    } while (self.hasNext())
    ParseSuccess(self, collection)
}

fun <T, A> many(parser: Parser<T, A>): Parser<T, List<A>> = manyTo({ mutableListOf() }, parser)

@JvmName("manyUnit")
fun <T> many(parser: Parser<T, Unit>): Parser<T, Unit> = Parser {
    var self = it
    do {
        when (val current = parser(self)) {
            is ParseSuccess -> {
                if (current.rest === self) break // do not attempt to repeat non-consuming parsers
                self = current.rest
            }
            is ParseFailure -> break
            is ParseError -> return@Parser current
        }
    } while (self.hasNext())
    ParseSuccess(self, Unit)
}

inline fun <T, A, M : MutableCollection<A>> manyOneTo(
    crossinline collectionFactory: () -> M,
    parser: Parser<T, A>
): Parser<T, M> = parser.flatMap { first ->
    val collection = collectionFactory()
    collection.add(first)
    manyTo({ collection }, parser)
}


fun <T, A> manyOne(parser: Parser<T, A>): Parser<T, List<A>> = manyOneTo({ mutableListOf() }, parser)

fun <T, A> optional(parser: Parser<T, A>): Parser<T, A?> = namedParser("$parser?") {
    when (val res = parser(it)) {
        is ParseFailure -> it.success(null)
        else -> res
    }
}
