package ru.spbstu

internal interface NamedParser<T, R>: Parser<T, R> {
    val name: String

    fun Input<T>.failure() = failure(name)
}

abstract class AbstractNamedParser<T, R>(override val name: String): NamedParser<T, R>
