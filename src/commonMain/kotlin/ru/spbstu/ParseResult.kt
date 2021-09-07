package ru.spbstu

sealed class ParseResult<out T, out R>
data class ParseSuccess<out T, out R>(val rest: Input<T>, val result: R): ParseResult<T, R>()
sealed class NoSuccess: ParseResult<Nothing, Nothing>() {
    abstract val expected: String
    abstract val found: String
    abstract val location: Location<*>
}

data class ParseFailure(override val expected: String,
                        override val found: String,
                        override val location: Location<*>): NoSuccess()

data class ParseError(override val expected: String,
                      override val found: String,
                      override val location: Location<*>): NoSuccess()

class ParseException(val result: NoSuccess): Exception("Failed to parse input: $result")

val <T, R> ParseResult<T, R>.successOrThrow: ParseSuccess<T, R>
    get() = when(this) {
        is ParseSuccess -> this
        is NoSuccess -> throw ParseException(this)
    }

val <T, R> ParseResult<T, R>.resultOrThrow: R
    get() = successOrThrow.result

val <T, R> ParseResult<T, R>.restOrThrow: Input<T>
    get() = successOrThrow.rest

inline fun <T, A, B> ParseResult<T, A>.map(body: (A) -> B): ParseResult<T, B> = when(this) {
    is ParseSuccess -> ParseSuccess(rest, body(result))
    is NoSuccess -> this
}

inline fun <T, A, B> ParseResult<T, A>.flatMap(body: (A) -> ParseResult<T, B>): ParseResult<T, B> = when(this) {
    is ParseSuccess -> body(result)
    is NoSuccess -> this
}

inline fun <T, A, B> ParseResult<T, A>.chain(body: (A, Input<T>) -> ParseResult<T, B>): ParseResult<T, B> = when(this) {
    is ParseSuccess -> body(result, rest)
    is NoSuccess -> this
}
