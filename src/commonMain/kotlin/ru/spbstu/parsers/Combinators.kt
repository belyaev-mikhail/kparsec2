package ru.spbstu.parsers

import ru.spbstu.*

inline fun <T, A, B> Parser<T, A>.map(crossinline body: (A) -> B): Parser<T, B> = Parser {
    this(it).map(body)
}

inline fun <T, A, B> Parser<T, A>.flatMap(crossinline body: (A) -> Parser<T, B>): Parser<T, B> = Parser {
    when(val res = this(it)) {
        is NoSuccess -> res
        is ParseSuccess -> {
            val (rest, result) = res
            body(result)(rest)
        }
    }
}

inline fun <T, A> Parser<T, A>.filter(crossinline body: (A) -> Boolean): Parser<T, A> = Parser {
    when(val res = this(it)) {
        is NoSuccess -> res
        is ParseSuccess -> {
            if (body(res.result)) {
                res
            } else {
                it.failure("body", "")
            }
        }
    }
}
