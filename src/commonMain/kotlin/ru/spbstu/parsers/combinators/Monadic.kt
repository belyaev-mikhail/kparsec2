package ru.spbstu.parsers.combinators

import ru.spbstu.*

inline fun <T, A, B> Parser<T, A>.map(crossinline body: (A) -> B): Parser<T, B> = namedParser("$this") {
    this@map(it).map(body)
}

inline fun <T, A, B> Parser<T, A>.flatMap(crossinline body: (A) -> Parser<T, B>): Parser<T, B> = Parser {
    this(it).chain { result, rest -> body(result)(rest) }
}

inline fun <T, A> Parser<T, A>.filter(
        expected: String = "($this)@filtered",
        crossinline body: (A) -> Boolean): Parser<T, A> = namedParser(expected) {
    val trye = this@filter(it)
    trye.flatMap { result -> if (body(result)) trye else it.failure(expected) }
}

inline fun <T, A, B> Parser<T, A>.mapNotNull(
        expected: String = "($this)@filtered",
        crossinline body: (A) -> B?): Parser<T, B> = namedParser(expected) { input ->
    when(val tryout = this@mapNotNull(input)) {
        is NoSuccess -> tryout
        is ParseSuccess -> {
            when(val mapped = body(tryout.result)) {
                null -> input.failure(expected)
                else -> tryout.map { mapped }
            }
        }
    }
}

inline fun <T, A, B, R> zipWith(left: Parser<T, A>,
                                right: Parser<T, B>,
                                crossinline body: (A, B) -> R): Parser<T, R> =
    namedParser({"$left + $right"}) {
        left(it).chain { l, it ->
            right(it).chain { r, it ->
                it.success(body(l, r))
            }
        }
    }

inline fun <T, A, B, C, R> zipWith(left: Parser<T, A>,
                                   mid: Parser<T, B>,
                                   right: Parser<T, C>,
                                   crossinline body: (A, B, C) -> R): Parser<T, R> =
    namedParser({"$left + $mid + $right"}) {
        left(it).chain { l, it ->
            mid(it).chain { m, it ->
                right(it).chain { r, it ->
                    it.success(body(l, m, r))
                }
            }
        }
    }

inline fun <T, A, B, C, D, R> zipWith(ll: Parser<T, A>,
                                      lm: Parser<T, B>,
                                      rm: Parser<T, C>,
                                      rr: Parser<T, D>,
                                      crossinline body: (A, B, C, D) -> R): Parser<T, R> =
    namedParser({"$ll + $lm + $rm + $rr"}) {
        ll(it).chain { e1, it ->
            lm(it).chain { e2, it ->
                rm(it).chain { e3, it ->
                    rr(it).chain { e4, it -> it.success(body(e1, e2, e3, e4)) }
                }
            }
        }
    }
