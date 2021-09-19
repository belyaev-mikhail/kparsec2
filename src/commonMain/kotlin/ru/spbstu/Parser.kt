package ru.spbstu

fun interface Parser<T, out R> {
    operator fun invoke(input: Input<T>): ParseResult<T, R>

    companion object
}

operator fun <R> Parser<Char, R>.invoke(input: String): ParseResult<Char, R> = invoke(stringInput(input))
