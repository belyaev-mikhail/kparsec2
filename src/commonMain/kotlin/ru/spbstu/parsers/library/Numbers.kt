package ru.spbstu.parsers.library

import ru.spbstu.parsers.combinators.map
import ru.spbstu.parsers.manyOneAsString
import ru.spbstu.parsers.oneOf

object Numbers {
    val binaryDigit = oneOf('0', '1')
    val decimalDigit = oneOf('0'..'9')
    val hexDigit = oneOf(('0'..'9') + ('a'..'f') + ('A'..'F'))

    val binaryNumber = manyOneAsString(binaryDigit).map { "$it".toInt(2) }
    val decimalNumber = manyOneAsString(decimalDigit).map { "$it".toInt() }
    val hexNumber = manyOneAsString(hexDigit).map { "$it".toInt(16) }

    val binaryLongNumber = manyOneAsString(binaryDigit).map { "$it".toLong(2) }
    val decimalLongNumber = manyOneAsString(decimalDigit).map { "$it".toLong() }
    val hexLongNumber = manyOneAsString(hexDigit).map { "$it".toLong(16) }
}