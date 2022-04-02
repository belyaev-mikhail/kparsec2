package ru.spbstu.parsers.combinators

import ru.spbstu.*
import kotlin.jvm.JvmName

private sealed interface Element<T, E> {
    interface ElementVisitor<T, E> {
        fun visitNo(parser: Parser<T, Unit>)
        fun visitOne(parser: Parser<T, E>)
        fun visitMany(parser: Parser<T, Iterable<E>>)
    }

    abstract infix fun accept(visitor: ElementVisitor<T, E>)
}
private data class NoElement<T, E>(val parser: Parser<T, Unit>): Element<T, E> {
    override fun accept(visitor: Element.ElementVisitor<T, E>) = visitor.visitNo(parser)

    override fun toString(): String = parser.toString()
}
private data class OneElement<T, E>(val parser: Parser<T, E>): Element<T, E> {
    override fun accept(visitor: Element.ElementVisitor<T, E>) = visitor.visitOne(parser)
    override fun toString(): String = parser.toString()
}
private data class ManyElements<T, E>(val parser: Parser<T, Iterable<E>>): Element<T, E> {
    override fun accept(visitor: Element.ElementVisitor<T, E>) = visitor.visitMany(parser)
    override fun toString(): String = parser.toString()
}

private inline fun <T, E> Collection<Element<T, E>>.process(
    input: Input<T>,
    onNone: () -> Unit = {},
    onOne: (E) -> Unit = {},
    onMany: (Iterable<E>) -> Unit = {}
): ParseResult<T, Unit> {
    var currentInput = input
    for (element in this) {
        when(element) {
            is NoElement -> {
                when (val res = element.parser(currentInput)) {
                    is NoSuccess -> return res
                    is ParseSuccess -> {
                        onNone()
                        currentInput = res.rest
                    }
                }

            }
            is OneElement -> {
                when (val res = element.parser(currentInput)) {
                    is NoSuccess -> return res
                    is ParseSuccess -> {
                        onOne(res.result)
                        currentInput = res.rest
                    }
                }
            }
            is ManyElements -> {
                when (val res = element.parser(currentInput)) {
                    is NoSuccess -> return res
                    is ParseSuccess -> {
                        onMany(res.result)
                        currentInput = res.rest
                    }
                }
            }
        }
    }
    return currentInput.unitSuccess()
}


private abstract class ZipToCollectionParser<T, E, R>(val elements: List<Element<T, E>>):
    Parser<T, R>,
    AbstractNamedParser<T, R>(elements.joinToString(" + ")) {

    abstract override fun invoke(input: Input<T>): ParseResult<T, R>
}

private inline fun <T, E, R> ZipToCollectionParser(
        elements: List<Element<T, E>>,
        crossinline body: ZipToCollectionParser<T, E, R>.(Input<T>) -> ParseResult<T, R>
) = object : ZipToCollectionParser<T, E, R>(elements) {
    override fun invoke(input: Input<T>): ParseResult<T, R> = body(input)
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

    private fun buildNoParser(): Parser<T, Unit> {
        return ZipToCollectionParser(collection) { input -> elements.process(input) }
    }

    private fun buildSingleParser(): Parser<T, E> {
        return ZipToCollectionParser(collection) { input ->
            var result: E? = null
            elements.process(input, onOne = {
                result = it
            }).map { result!! }
        }
    }
    private fun buildSingleCollectionParser(): Parser<T, Iterable<E>> {
        return ZipToCollectionParser(collection) { input ->
            var result: Iterable<E>? = null
            elements.process(input, onMany = {
                result = it
            }).map { result!! }
        }
    }

    private fun buildGeneralParser(): Parser<T, List<E>> {
        return ZipToCollectionParser(collection) { input ->
            val resultCollection: MutableList<E> = mutableListOf()
            elements.process(
                input,
                onOne = { resultCollection.add(it) },
                onMany = { resultCollection.addAll(it) }
            ).map { resultCollection as List<E> }
        }
    }

    fun build(): Parser<T, *> {
        val ones = collection.count { it is OneElement }
        val manys = collection.count { it is ManyElements }
        return when {
            ones == 0 && manys == 0 -> buildNoParser()
            ones == 1 && manys == 0 -> buildSingleParser()
            ones == 0 && manys == 1 -> buildSingleCollectionParser()
            else -> buildGeneralParser()
        }
    }
}

inline fun <T, E> zipToCollectionParser(body: ZipToCollectionBuilder<T, E>.() -> Unit): Parser<T, *> =
    ZipToCollectionBuilder<T, E>().apply(body).build()
