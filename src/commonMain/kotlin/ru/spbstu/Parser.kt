package ru.spbstu

fun interface Parser<in T, out R> {
    operator fun invoke(input: Input<T>): ParseResult<@UnsafeVariance T, R>

    companion object
}

operator fun <R> Parser<Char, R>.invoke(input: String): ParseResult<Char, R> = invoke(stringInput(input))
