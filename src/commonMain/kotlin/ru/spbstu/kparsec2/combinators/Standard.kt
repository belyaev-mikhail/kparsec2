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

// this is only for disambiguation with Set<T> overload
fun <T, A> oneOf(parsers: Set<Parser<T, A>>): Parser<T, A> = oneOf(parsers as Iterable<Parser<T, A>>)

fun <T, A> oneOf(vararg parsers: Parser<T, A>): Parser<T, A> = oneOf(parsers.asList())

abstract class SequenceFoldParser<T, A, Ctx>(val parsers: Iterable<Parser<T, A>>):
        Parser<T, Ctx>,
    NamedParser<T, Ctx> {
    abstract fun initialContext(): Ctx
    abstract fun body(ctx: Ctx, value: A): Ctx

    override val name: String = parsers.joinToString(" + ")
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

private fun manyFoldParserName(base: Any?, minIterations: Int, maxIterations: Int) = when {
    minIterations == maxIterations -> "($base) × $minIterations"
    minIterations == 0 && maxIterations == Int.MAX_VALUE -> "($base)*"
    minIterations == 1 && maxIterations == Int.MAX_VALUE -> "($base)+"
    else -> "($base) × ($minIterations..$maxIterations)"
}

abstract class ManyFoldParser<T, A, Ctx>(val base: Parser<T, A>,
                                         val minIterations: Int = 0,
                                         val maxIterations: Int = Int.MAX_VALUE):
        Parser<T, Ctx>,
        AbstractNamedParser<T, Ctx>(manyFoldParserName(base, minIterations, maxIterations)) {
    abstract fun initialContext(): Ctx
    abstract fun body(ctx: Ctx, currentResult: A): Ctx

    fun isUnbounded(): Boolean = maxIterations == Int.MAX_VALUE

    override fun invoke(input: Input<T>): ParseResult<T, Ctx> {
        var self = input
        var context = initialContext()
        var iterations = 0
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
            ++iterations
        } while (self.hasNext() && iterations < maxIterations)
        if (iterations < minIterations) return input.failure("($base) * $minIterations", context)
        return ParseSuccess(self, context)
    }
}

@PublishedApi
internal inline
fun <T, A, Ctx> manyFold(crossinline initialContext: () -> Ctx,
                         parser: Parser<T, A>,
                         crossinline body: Ctx.(A) -> Ctx): Parser<T, Ctx> =
    when {
        parser is ManyFoldParser<*, *, *> && parser.isUnbounded() ->
            (parser as Parser<T, A>).map { body(initialContext(), it) }
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
    when {
        parser is ManyFoldParser<*, *, *> && parser.isUnbounded() ->
            (parser as Parser<T, A>).map { body(initialContext(), it) }
        else -> object: ManyFoldParser<T, A, Ctx>(parser, minIterations = 1) {
            override fun initialContext(): Ctx = initialContext.invoke()
            override fun body(ctx: Ctx, currentResult: A): Ctx = body.invoke(ctx, currentResult)
        }
    }

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
    namedParser({ "($base)?" }) {
        when (val res = base(it)) {
            is ParseFailure -> it.success(defaultValue())
            else -> res
        }
    }

fun <T, A> optional(parser: Parser<T, A>): Parser<T, A?> = recover(parser) { null }

fun <T, A> repeat(n: Int, parser: Parser<T, A>): Parser<T, List<A>> = when {
    n == 0 -> success(listOf())
    n == 1 -> parser.map { listOf(it) }
    parser is ManyFoldParser<*, *, *> && parser.isUnbounded() ->
        throw IllegalArgumentException("repeat() used on many() which is not what you probably tried to achieve")
    else -> object: ManyFoldParser<T, A, MutableList<A>>(parser, minIterations = n, maxIterations = n) {
        override fun initialContext() = mutableListOf<A>()
        override fun body(ctx: MutableList<A>, currentResult: A): MutableList<A> {
            ctx.add(currentResult)
            return ctx
        }
    }
}

abstract class SeparatedByFoldParser<T, A, Ctx>(val base: Parser<T, A>, val sep: Parser<T, Unit>):
        Parser<T, Ctx>,
        AbstractNamedParser<T, Ctx>("($base){$sep}") {
    abstract fun initialContext(): Ctx
    abstract fun body(ctx: Ctx, currentResult: A): Ctx

    override fun invoke(input: Input<T>): ParseResult<T, Ctx> {
        var self = input
        var context = initialContext()

        when (val first = base(self)) {
            is ParseSuccess -> {
                context = body(context, first.result)
                self = first.rest
            }
            is ParseFailure -> return ParseSuccess(self, context)
            is ParseError -> return first
        }

        while (self.hasNext()) {
            val separator = sep(self)
            when (separator) {
                is ParseSuccess -> {}
                is ParseFailure -> break
                is ParseError -> return separator
            }
            when (val current = base(separator.rest)) {
                is ParseSuccess -> {
                    context = body(context, current.result)
                    self = current.rest
                }
                is ParseFailure -> break
                is ParseError -> return current
            }
        }
        return ParseSuccess(self, context)
    }
}

fun <T, A> separatedBy(base: Parser<T, A>, sep: Parser<T, Unit>): Parser<T, List<A>> =
    object : SeparatedByFoldParser<T, A, MutableList<A>>(base, sep) {
        override fun initialContext(): MutableList<A> = mutableListOf()
        override fun body(ctx: MutableList<A>, currentResult: A): MutableList<A> =
            ctx.apply { add(currentResult) }
    }
