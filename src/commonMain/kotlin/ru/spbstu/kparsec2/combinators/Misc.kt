package ru.spbstu.kparsec2.parsers.combinators

import ru.spbstu.kparsec2.*

infix fun <T, A> Parser<T, A>.named(name: String) = namedParser(name, this)

infix fun <T, A> Parser<T, A>.named(lazyName: () -> String) = object : NamedParser<T, A> {
    override val name: String
        get() = lazyName()

    override fun toString(): String = name
    override fun invoke(input: Input<T>): ParseResult<T, A> = this@named.invoke(input)
}

fun <T, A> namedParser(name: String, parser: Parser<T, A>): Parser<T, A> =
    object : Parser<T, A> by parser, AbstractNamedParser<T, A>(name) {}

inline fun <T, A> namedParser(
    name: String,
    crossinline parser: NamedParser<T, A>.(input: Input<T>) -> ParseResult<@UnsafeVariance T, A>
): Parser<T, A> = object : AbstractNamedParser<T, A>(name) {
    override fun invoke(input: Input<T>): ParseResult<T, A> = parser(input)
}

inline fun <T, A> namedParser(
    crossinline lazyName: () -> String,
    crossinline parser: NamedParser<T, A>.(input: Input<T>) -> ParseResult<@UnsafeVariance T, A>
): Parser<T, A> = object : NamedParser<T, A> {
    override val name: String
        get() = lazyName()
    override fun invoke(input: Input<T>): ParseResult<T, A> = parser(input)
    override fun toString(): String = name
}

fun <T, A> Parser<T, A>.ignoreResult(): Parser<T, Unit> = map { Unit }

private class LazyParser<T, R>(val lz: Lazy<Parser<T, R>>): NamedParser<T, R> {
    val defer
        get() = lz.value
    override fun invoke(input: Input<T>): ParseResult<T, R> = defer(input)
    override val name: String
        get() = if (lz.isInitialized()) "$defer" else "<not yet initialized>"
    override fun toString(): String = name
}

fun <T, R> lazyParser(body: () -> Parser<T, R>): Parser<T, R> {
    val lz = lazy(body)
    return LazyParser(lz)
}

fun <T, R> recursive(body: (Parser<T, R>) -> Parser<T, R>): Parser<T, R> {
    var capture: Parser<T, R>? = null
    val lz = lazy { body(capture!!) }
    capture = LazyParser(lz)
    return capture
}
