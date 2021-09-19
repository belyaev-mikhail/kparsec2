package ru.spbstu.parsers.dsl

import ru.spbstu.Parser
import ru.spbstu.parsers.combinators.*
import kotlin.jvm.JvmName

operator fun <T, R> Parser<T, Iterable<R>>.plus(that: Parser<T, Iterable<R>>): Parser<T, List<R>> =
    zipWith(this, that) { a, b -> a + b }

@JvmName("plusSingle")
operator fun <T, R> Parser<T, Iterable<R>>.plus(that: Parser<T, R>): Parser<T, List<R>> =
    zipWith(this, that) { a, b -> a + b }

@JvmName("plusSingleReversed")
operator fun <T, R> Parser<T, R>.plus(that: Parser<T, Iterable<R>>): Parser<T, List<R>> =
    zipWith(this, that) { a, b -> mutableListOf(a).apply { addAll(b) } as List<R> }

@JvmName("plusSingleSingle")
operator fun <T, R> Parser<T, R>.plus(that: Parser<T, R>): Parser<T, List<R>> =
    zipWith(this, that) { a, b -> listOf(a, b) }

@JvmName("plusUnit")
operator fun <T, I: Iterable<Any?>> Parser<T, I>.plus(that: Parser<T, Unit>): Parser<T, I> =
    zipWith(this, that) { a, _ -> a }

@JvmName("plusUnitReversed")
operator fun <T, I: Iterable<Any?>> Parser<T, Unit>.plus(that: Parser<T, I>): Parser<T, I> =
    zipWith(this, that) { _, b -> b }

@JvmName("single-plus-unit")
operator fun <T, R> Parser<T, R>.plus(that: Parser<T, Unit>): Parser<T, R> =
    zipWith(this, that) { a, _ -> a }

@JvmName("unit-plus-single")
operator fun <T, R> Parser<T, Unit>.plus(that: Parser<T, R>): Parser<T, R> =
    zipWith(this, that) { _, b -> b }

@JvmName("unit-plus-unit")
operator fun <T> Parser<T, Unit>.plus(that: Parser<T, Unit>): Parser<T, Unit> =
    zipWith(this, that) { _, _ -> }

operator fun <T, R> Parser<T, R>.unaryMinus(): Parser<T, Unit> = ignoreResult()

infix fun <T, R> Parser<T, R>.or(that: Parser<T, R>): Parser<T, R> = oneOf(this, that)

operator fun <T, R> Parser<T, R>.not(): Parser<T, Unit> = not(this)

operator fun <T, R> Parser<T, R>.times(n: Int): Parser<T, List<R>> = repeat(n, this)

infix fun <T, R> Parser<T, R>.sepBy(separator: Parser<T, Unit>): Parser<T, List<R>> = separatedBy(this, separator)

inline infix fun <T, R> Parser<T, R>.orElse(crossinline default: () -> R): Parser<T, R> =
    recover(this, default)

