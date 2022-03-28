package ru.spbstu.parsers.combinators

import ru.spbstu.*
import kotlin.jvm.JvmName

private sealed interface Element<T, E> {
    operator fun invoke(input: Input<T>, acc: MutableList<E>): ParseResult<T, Unit>
}
private data class NoElement<T, E>(val parser: Parser<T, Unit>): Element<T, E> {
    override fun invoke(input: Input<T>, acc: MutableList<E>): ParseResult<T, Unit> =
        parser(input)

    override fun toString(): String = parser.toString()
}
private data class OneElement<T, E>(val parser: Parser<T, E>): Element<T, E> {
    override fun invoke(input: Input<T>, acc: MutableList<E>): ParseResult<T, Unit> =
        when (val res = parser(input)) {
            is ParseSuccess -> {
                acc.add(res.result)
                res.ignoreResult()
            }
            is NoSuccess -> res
        }
    override fun toString(): String = parser.toString()
}
private data class ManyElements<T, E>(val parser: Parser<T, Iterable<E>>): Element<T, E> {
    override fun invoke(input: Input<T>, acc: MutableList<E>): ParseResult<T, Unit> =
        when (val res = parser(input)) {
            is ParseSuccess -> {
                acc.addAll(res.result)
                res.ignoreResult()
            }
            is NoSuccess -> res
        }
    override fun toString(): String = parser.toString()
}

private abstract class ZipToCollectionParser<T, E, R>(val elements: List<Element<T, E>>):
    Parser<T, R>,
    AbstractNamedParser<T, R>(elements.joinToString(" + ")) {

    abstract override fun invoke(input: Input<T>): ParseResult<T, R>

    fun invoke(input: Input<T>, acc: MutableList<E>): ParseResult<T, Unit> {
        var currentInput = input
        for (element in elements) {
            when(val res = element(currentInput, acc)) {
                is NoSuccess -> return res
                is ParseSuccess -> {
                    currentInput = res.rest
                }
            }
        }
        return ParseSuccess(currentInput, Unit)
    }
}

private class ZipToCollectionParserToCollection<T, E>(elements: List<Element<T, E>>):
        ZipToCollectionParser<T, E, List<E>>(elements) {
    override fun invoke(input: Input<T>): ParseResult<T, List<E>> {
        val acc = mutableListOf<E>()
        return invoke(input, acc).map { acc }
    }
}

private class ZipToCollectionParserToSingle<T, E>(elements: List<Element<T, E>>):
    ZipToCollectionParser<T, E, E>(elements) {
    override fun invoke(input: Input<T>): ParseResult<T, E> {
        val acc = mutableListOf<E>()
        return invoke(input, acc).map { acc.single() }
    }
}

class ZipToCollectionBuilder<T, E> {
    private val collection: MutableList<Element<T, E>> = mutableListOf()
    @JvmName("addNoElementParser")
    fun addParser(noElement: Parser<T, Unit>) {
        collection.add(NoElement(noElement))
    }
    @JvmName("addOneElementParser")
    fun addParser(oneElement: Parser<T, E>) {
        if (oneElement is ZipToCollectionParser<*, *, *>) {
            @Suppress("UNCHECKED_CAST") // it really cannot be anything else
            oneElement as ZipToCollectionParser<T, E, E>
            collection.addAll(oneElement.elements)
        } else {
            collection.add(OneElement(oneElement))
        }
    }

    @JvmName("addParserCollection")
    fun addParser(manyElements: Parser<T, Iterable<E>>) {
        if (manyElements is ZipToCollectionParser<*, *, *>) {
            @Suppress("UNCHECKED_CAST") // it really cannot be anything else
            manyElements as ZipToCollectionParser<T, E, List<E>>
            collection.addAll(manyElements.elements)
        } else {
            collection.add(ManyElements(manyElements))
        }
    }

    fun build(): Parser<T, List<E>> = ZipToCollectionParserToCollection(collection)
    fun buildSingle(): Parser<T, E> = ZipToCollectionParserToSingle(collection)
}

inline fun <T, E> zipToCollectionParser(body: ZipToCollectionBuilder<T, E>.() -> Unit): Parser<T, List<E>> =
    ZipToCollectionBuilder<T, E>().apply(body).build()

inline fun <T, E> zipToCollectionParserSingle(body: ZipToCollectionBuilder<T, E>.() -> Unit): Parser<T, E> =
    ZipToCollectionBuilder<T, E>().apply(body).buildSingle()
