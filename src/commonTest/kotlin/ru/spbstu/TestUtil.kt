package ru.spbstu

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertTrue

@OptIn(ExperimentalContracts::class)
fun <T, R> assertSuccess(parseResult: ParseResult<T, R>) {
    contract { returns() implies (parseResult is ParseSuccess) }
    assertTrue(parseResult is ParseSuccess, "Expected success, but received $parseResult")
}

// explicitly "turn off" smartcasts for this piece of code
inline fun runIsolated(body: () -> Unit) = body()
