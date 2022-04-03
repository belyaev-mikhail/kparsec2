package ru.spbstu

import kotlinx.warnings.Warnings
import ru.spbstu.Source.Companion.dropDefault

abstract class Input<out T>(val source: Source<T>): Source<T> by source {
    abstract val location: Location<*>
    abstract override fun advance(): Input<T>
    abstract override fun drop(n: Int): Input<T>
}

class SimpleInput<out T>(source: Source<T>, override val location: Location<@UnsafeVariance T>): Input<T>(source) {
    override fun advance(): SimpleInput<T> {
        val current = source.currentOrNull ?: return this
        return SimpleInput(source.advance(), location(current))
    }

    override fun drop(n: Int): SimpleInput<T> = when {
        location.canSkip -> {
            SimpleInput(source.drop(n), location.skip(n))
        }
        else -> {
            dropDefault(n)
        }
    }

    override fun toString(): String {
        return "SimpleInput(source=$source,location=$location)"
    }
}

private fun <T> Input<T>.actualCurrentTokenDesc() = when {
    !hasNext() -> "<end of input>"
    else -> "$current"
}
fun <T> Input<T>.failure(expected: String, actual: String = actualCurrentTokenDesc()): ParseFailure =
    ParseFailure(expected, actual, location)
fun <T> Input<T>.error(expected: String, actual: String = actualCurrentTokenDesc()): ParseError =
    ParseError(expected, actual, location)
fun <T> Input<T>.unitSuccess() = ParseSuccess(this, Unit)
fun <T, R> Input<T>.success(value: R) = ParseSuccess(this, value)

fun <T> Input<T>.readCurrentToken(): ParseResult<T, T> {
    if (!hasNext()) return failure("<any token>", "<end of input>")
    val token = current
    return advance().success(token)
}

class ParsedInput<T, R>(source: ParsedSource<T, R>): Input<R>(source) {
    @Suppress(Warnings.UNCHECKED_CAST)
    internal val parsedSource
        inline get() = source as ParsedSource<T, R>

    override val location: Location<*>
        get() = parsedSource.input.location

    override fun advance(): ParsedInput<T, R> = ParsedInput(parsedSource.advance())
    override fun drop(n: Int): ParsedInput<T, R> = ParsedInput(parsedSource.drop(n))
}

enum class CharLocationType {
    LINES {
        override fun start(source: Source<Char>): Location<Char> = CharLocation()
    },
    OFFSET {
        override fun start(source: Source<Char>): Location<Char> = OffsetLocation()
    },
    MANAGED {
        override fun start(source: Source<Char>): Location<Char> = managedCharLocation(source)
    };

    abstract fun start(source: Source<Char>): Location<Char>
}

fun stringInput(data: String, locationType: CharLocationType = CharLocationType.LINES) =
    SimpleInput(StringSource(data), CharLocation())
fun <T0, T1> parsedInput(input: Input<T0>, parser: Parser<T0, T1>, ignore: Parser<T0, Unit>? = null): Input<T1> =
    ParsedInput(ParsedSource(input, parser, ignore))
