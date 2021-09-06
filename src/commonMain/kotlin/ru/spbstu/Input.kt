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
        val current = source.current
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
}

fun <T> Input<T>.failure(expected: String, actual: String): Failure = Failure(expected, actual, location)
fun <T> Input<T>.error(expected: String, actual: String): Error = Error(expected, actual, location)
fun <T> Input<T>.unitSuccess() = ParseSuccess(this, Unit)

class ParsedInput<T, R>(source: ParsedSource<T, R>): Input<R>(source) {
    @Suppress(Warnings.UNCHECKED_CAST)
    private val parsedSource
        inline get() = source as ParsedSource<T, R>

    override val location: Location<*>
        get() = parsedSource.input.location

    override fun advance(): ParsedInput<T, R> = ParsedInput(parsedSource.advance())
    override fun drop(n: Int): ParsedInput<T, R> = ParsedInput(parsedSource.drop(n))
}

fun stringInput(data: String) = SimpleInput(StringSource(data), CharLocation())
