package ru.spbstu

import kotlinx.warnings.Warnings

fun interface Parser<T, out R> {
    operator fun invoke(input: Input<T>): ParseResult<T, R>

    companion object
}
