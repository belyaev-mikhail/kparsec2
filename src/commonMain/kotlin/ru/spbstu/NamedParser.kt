package ru.spbstu

interface NamedParser<T, R>: Parser<T, R> {
    val name: String
    override fun toString(): String

    fun Input<T>.failure() = failure(name)
}

abstract class AbstractNamedParser<T, R>(override val name: String): NamedParser<T, R> {
    override fun toString(): String = name
}
