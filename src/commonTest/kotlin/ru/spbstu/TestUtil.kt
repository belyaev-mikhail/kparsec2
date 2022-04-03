package ru.spbstu

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalContracts::class)
fun <T, R> assertSuccess(parseResult: ParseResult<T, R>) {
    contract { returns() implies (parseResult is ParseSuccess) }
    assertTrue(parseResult is ParseSuccess, "Expected success, but received $parseResult")
}

// explicitly "turn off" smartcasts for this piece of code
inline fun runIsolated(body: () -> Unit) = body()

fun <T, R> assertEqualResults(expected: ParseResult<T, R>, actual: ParseResult<T, R>,
                              deepCompareErrors: Boolean = true) {
    try {
        when (expected) {
            is ParseSuccess -> {
                assertTrue(actual is ParseSuccess)
                assertEquals(expected.result, actual.result)
                assertEquals(expected.rest.location, actual.rest.location)
            }
            is NoSuccess -> when {
                !deepCompareErrors -> {
                    assertEquals(expected::class, actual::class)
                }
                else -> {
                    assertEquals(expected, actual)
                }
            }
        }
    } catch (assertionError: AssertionError) {
        assertEquals(expected, actual)
    }

}
