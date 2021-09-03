package ru.spbstu

import kotlinx.warnings.Warnings

sealed class ParseResult<out T, out R>
data class ParseSuccess<out T, out R>(val rest: Input<T>, val result: R): ParseResult<T, R>()
sealed class NoSuccess: ParseResult<Nothing, Nothing>() {
    abstract val expected: String
    abstract val found: String
    abstract val location: Location<*>
}

data class Failure(override val expected: String,
                   override val found: String,
                   override val location: Location<*>): NoSuccess()

data class Error(override val expected: String,
                 override val found: String,
                 override val location: Location<*>): NoSuccess()

class ParseException(result: NoSuccess): Exception("Failed to parse input: $result")

val <T, R> ParseResult<T, R>.successOrThrow: ParseSuccess<T, R>
    get() = when(this) {
        is ParseSuccess -> this
        is NoSuccess -> throw ParseException(this)
    }

val <T, R> ParseResult<T, R>.resultOrThrow: R
    get() = successOrThrow.result

val <T, R> ParseResult<T, R>.restOrThrow: Input<T>
    get() = successOrThrow.rest

fun interface Parser<T, out R> {
    operator fun invoke(input: Input<T>): ParseResult<T, R>
}
