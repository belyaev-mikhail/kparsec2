package ru.spbstu.parsers.util

class StringBuilderCollection(val stringBuilder: StringBuilder = StringBuilder()): AbstractMutableCollection<Char>() {
    override val size: Int
        get() = stringBuilder.length

    override fun add(element: Char): Boolean {
        stringBuilder.append(element)
        return true
    }

    private inner class TheIterator : MutableIterator<Char> {
        private var index = 0
        override fun hasNext(): Boolean = index < size
        override fun next(): Char = stringBuilder[index++]
        override fun remove() { stringBuilder.deleteAt(index) }
    }

    override fun iterator(): MutableIterator<Char> = TheIterator()
}

fun StringBuilderCollection.concatToString() = stringBuilder.toString()

fun StringBuilder.asCollection(): StringBuilderCollection = StringBuilderCollection(this)
