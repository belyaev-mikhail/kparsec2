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

internal inline fun <T, A, R> manyAndForEach(
    result: R,
    parser: Parser<T, A>,
    crossinline body: (A) -> Unit
): Parser<T, R> = Parser {
    var self = it
    do {
        when (val current = parser(self)) {
            is ParseSuccess -> {
                body(current.result)
                if (current.rest === self) break // do not attempt to repeat non-consuming parsers
                self = current.rest
            }
            is ParseFailure -> break
            is ParseError -> return@Parser current
        }
    } while (self.hasNext())
    self.success(result)
}

internal inline fun <T, A, R> manyOneAndForEach(
    result: R,
    parser: Parser<T, A>,
    crossinline body: (A) -> Unit
): Parser<T, R> = parser.flatMap {
    body(it)
    manyAndForEach(result, parser, body)
}

fun <T, A> many(parser: Parser<T, A>): Parser<T, List<A>> = run {
    val res = mutableListOf<A>()
    manyAndForEach(res, parser) { res.add(it) }
}

@JvmName("manyUnit")
fun <T> many(parser: Parser<T, Unit>): Parser<T, Unit> = manyAndForEach(Unit, parser) {}

inline fun <T, A, M : MutableCollection<A>> manyOneTo(
    crossinline collectionFactory: () -> M,
    parser: Parser<T, A>
): Parser<T, M> = parser.flatMap { first ->
    val collection = collectionFactory()
    collection.add(first)
    manyTo({ collection }, parser)
}


fun <T, A> manyOne(parser: Parser<T, A>): Parser<T, List<A>> = manyOneTo({ mutableListOf() }, parser)

inline fun <T, A> recover(base: Parser<T, A>, crossinline defaultValue: () -> A): Parser<T, A> =
    namedParser("$base?") {
        when (val res = base(it)) {
            is ParseFailure -> it.success(defaultValue())
            else -> res
        }
    }

fun <T, A> optional(parser: Parser<T, A>): Parser<T, A?> = recover(parser) { null }

fun <T, A> repeat(n: Int, parser: Parser<T, A>): Parser<T, List<A>> = namedParser("$parser * $n") { input ->
    if (n == 0) return@namedParser input.success(listOf())
    var currentResult = parser(input)
    val res = mutableListOf<A>()
    kotlin.repeat(n - 1) {
        val stableCurrentResult = currentResult
        if (stableCurrentResult !is ParseSuccess) return@namedParser input.failure("$parser")
        res += stableCurrentResult.result
        currentResult = parser(stableCurrentResult.rest)
    }
    currentResult.map { res.apply { add(it) } }
}

fun <T, A> separatedBy(base: Parser<T, A>, sep: Parser<T, Unit>): Parser<T, List<A>> =
    recover(
        base.flatMap {
            val acc = mutableListOf(it)
            manyAndForEach(acc, zipWith(sep, base) { _, r -> r }) { acc += it }
        }
    ) { emptyList() }
