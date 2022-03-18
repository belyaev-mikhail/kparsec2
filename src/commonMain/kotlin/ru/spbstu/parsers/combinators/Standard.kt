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

internal inline
fun <T, A, Ctx> sequenceFold(crossinline initialContext: () -> Ctx,
                             parsers: Iterable<Parser<T, A>>,
                             crossinline body: Ctx.(A) -> Ctx): Parser<T, Ctx> = Parser {
    var self = it
    var context = initialContext()
    for (element in parsers) {
        when (val current = element(self)) {
            is ParseSuccess -> {
                context = body(context, current.result)
                self = current.rest
            }
            is NoSuccess -> return@Parser current
        }
    }
    ParseSuccess(self, context)
}

internal inline
fun <T, A, Ctx> manyFold(crossinline initialContext: () -> Ctx,
                         parser: Parser<T, A>,
                         crossinline body: Ctx.(A) -> Ctx): Parser<T, Ctx> = namedParser("${parser}*") {
    var self = it
    var context = initialContext()
    do {
        when (val current = parser(self)) {
            is ParseSuccess -> {
                context = body(context, current.result)
                if (current.rest === self) break // do not attempt to repeat non-consuming parsers
                self = current.rest
            }
            is ParseFailure -> break
            is ParseError -> return@namedParser current
        }
    } while (self.hasNext())
    ParseSuccess(self, context)
}

internal inline
fun <T, A, Ctx> manyOneFold(crossinline initialContext: () -> Ctx,
                            parser: Parser<T, A>,
                            crossinline body: Ctx.(A) -> Ctx): Parser<T, Ctx> =
    namedParser("${parser}+", parser.flatMap {
        val ctx = body(initialContext(), it)
        manyFold({ ctx }, parser, body)
    })

fun <T, A> sequence(parsers: Iterable<Parser<T, A>>): Parser<T, List<A>> =
    sequenceFold({ mutableListOf() }, parsers) { apply { add(it) } }

fun <T, A> sequence(vararg parsers: Parser<T, A>): Parser<T, List<A>> = sequence(parsers.asList())

fun <T, A> many(parser: Parser<T, A>): Parser<T, List<A>> =
    manyFold({ mutableListOf() }, parser) { apply { add(it) } }

@JvmName("manyUnit")
fun <T> many(parser: Parser<T, Unit>): Parser<T, Unit> =
    manyFold({ Unit }, parser) {}

fun <T, A> manyOne(parser: Parser<T, A>): Parser<T, List<A>> =
    manyOneFold({ mutableListOf() }, parser) { apply { add(it) } }

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
            manyFold({ acc }, zipWith(sep, base) { _, r -> r }) { add(it); this }
        }
    ) { emptyList() }
