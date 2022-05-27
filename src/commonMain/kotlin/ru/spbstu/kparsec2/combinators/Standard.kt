@file:OptIn(ExperimentalStdlibApi::class)

package ru.spbstu.kparsec2.parsers.combinators

import ru.spbstu.kparsec2.*
import kotlin.jvm.JvmName

class OneOfParser<T, A>(val parsers: Iterable<Parser<T, A>>):
        Parser<T, A>,
        AbstractNamedParser<T, A>(parsers.joinToString(" | ")) {
    override fun invoke(input: Input<T>): ParseResult<T, A> {
        for (parser in parsers) {
            when (val res = parser(input)) {
                is ParseSuccess -> return res
                else -> continue
            }
        }
        return input.failure()
    }
}

fun <T, A> oneOf(parsers: Iterable<Parser<T, A>>): Parser<T, A> = OneOfParser(
    buildList {
        for (parser in parsers) when (parser) {
            is OneOfParser -> addAll(parser.parsers)
            else -> add(parser)
        }
    }
)

fun <T, A> oneOf(vararg parsers: Parser<T, A>): Parser<T, A> = oneOf(parsers.asList())

abstract class SequenceFoldParser<T, A, Ctx>(val parsers: Iterable<Parser<T, A>>):
        Parser<T, Ctx>,
    NamedParser<T, Ctx> {
    abstract fun initialContext(): Ctx
    abstract fun body(ctx: Ctx, value: A): Ctx

    override val name: String = parsers.joinToString(" ")
    override fun toString(): String = name

    override fun invoke(input: Input<T>): ParseResult<T, Ctx> {
        var self = input
        var context = initialContext()
        for (element in parsers) {
            when (val current = element(self)) {
                is ParseSuccess -> {
                    context = body(context, current.result)
                    self = current.rest
                }
                is NoSuccess -> return current
            }
        }
        return self.success(context)
    }
}

internal inline
fun <T, A, Ctx> sequenceFold(crossinline initialContext: () -> Ctx,
                             parsers: Iterable<Parser<T, A>>,
                             crossinline body: Ctx.(A) -> Ctx): Parser<T, Ctx> =
    object: SequenceFoldParser<T, A, Ctx>(parsers) {
        override fun initialContext(): Ctx = initialContext.invoke()
        override fun body(ctx: Ctx, value: A): Ctx = body.invoke(ctx, value)
    }

abstract class ManyFoldParser<T, A, Ctx>(val base: Parser<T, A>):
        Parser<T, Ctx>,
        AbstractNamedParser<T, Ctx>("$base*") {
    abstract fun initialContext(): Ctx
    abstract fun body(ctx: Ctx, currentResult: A): Ctx

    override fun invoke(input: Input<T>): ParseResult<T, Ctx> {
        var self = input
        var context = initialContext()
        do {
            when (val current = base(self)) {
                is ParseSuccess -> {
                    context = body(context, current.result)
                    if (current.rest === self) break // do not attempt to repeat non-consuming parsers
                    self = current.rest
                }
                is ParseFailure -> break
                is ParseError -> return current
            }
        } while (self.hasNext())
        return ParseSuccess(self, context)
    }
}

@PublishedApi
internal inline
fun <T, A, Ctx> manyFold(crossinline initialContext: () -> Ctx,
                         parser: Parser<T, A>,
                         crossinline body: Ctx.(A) -> Ctx): Parser<T, Ctx> =
    when (parser) {
        is ManyFoldParser<*, *, *> -> (parser as Parser<T, A>).map { body(initialContext(), it) }
        else -> object: ManyFoldParser<T, A, Ctx>(parser) {
            override fun initialContext(): Ctx = initialContext.invoke()
            override fun body(ctx: Ctx, currentResult: A): Ctx = body.invoke(ctx, currentResult)
        }
    }

@PublishedApi
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

// this exists only to disambiguiate resolution ambiguities with sequence(Collection<Token>)
fun <T, A> sequence(parsers: Collection<Parser<T, A>>): Parser<T, List<A>> =
    sequence(parsers as Iterable<Parser<T, A>>)

fun <T, A> sequence(vararg parsers: Parser<T, A>): Parser<T, List<A>> =
    sequence(parsers.asList())

fun <T, A> many(parser: Parser<T, A>): Parser<T, List<A>> =
    manyFold({ mutableListOf() }, parser) { apply { add(it) } }

@JvmName("manyUnit")
fun <T> many(parser: Parser<T, Unit>): Parser<T, Unit> =
    manyFold({ Unit }, parser) {}

fun <T, A> manyOne(parser: Parser<T, A>): Parser<T, List<A>> =
    manyOneFold({ mutableListOf() }, parser) { apply { add(it) } }

inline fun <T, A> recover(base: Parser<T, A>, crossinline defaultValue: () -> A): Parser<T, A> =
    namedParser({ "$base?" }) {
        when (val res = base(it)) {
            is ParseFailure -> it.success(defaultValue())
            else -> res
        }
    }

fun <T, A> optional(parser: Parser<T, A>): Parser<T, A?> = recover(parser) { null }

fun <T, A> repeat(n: Int, parser: Parser<T, A>): Parser<T, List<A>> = namedParser(lazyName = { "$parser * $n" }) { input ->
    if (n == 0) return@namedParser input.success(listOf())
    var currentResult = parser(input)
    val res = mutableListOf<A>()
    kotlin.repeat(n - 1) {
        val stableCurrentResult = currentResult
        if (stableCurrentResult !is ParseSuccess) return@namedParser input.failure(parser)
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
    ) { emptyList() } named { "($base){$sep}" }
