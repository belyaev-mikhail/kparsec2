package ru.spbstu.parsers.stateful

import ru.spbstu.CompoundInput
import ru.spbstu.ParseSuccess
import ru.spbstu.Parser
import ru.spbstu.parsers.combinators.namedParser
import ru.spbstu.success
import ru.spbstu.wheels.Option

inline fun <reified S> stateGet(): Parser<Any?, S> = namedParser("stateGet") { input ->
    when (input) {
        is CompoundInput -> input.success(input.getComponent<StatefulInput<S>>().state)
        else -> throw IllegalStateException("stateGet() called on non-stateful input")
    }
}

inline fun <reified S> stateSet(s: S): Parser<Any?, Unit> = namedParser("stateSet($s)") { input ->
    ParseSuccess(stateful(input, s), Unit)
}

inline fun <reified S> stateModify(crossinline body: (S) -> S): Parser<Any?, Unit> =
    namedParser("stateModify(..)") { input ->
        when (input) {
            is CompoundInput -> {
                val currentState = input.getComponent<StatefulInput<S>>().state
                ParseSuccess(stateful(input, body(currentState)), Unit)
            }
            else -> throw IllegalStateException("stateGet() called on non-stateful input")
        }
    }

inline fun <reified S> stateModifyIfSet(crossinline body: (Option<S>) -> S): Parser<Any?, Unit> =
    namedParser("stateModifyIfSet(..)") { input ->
        when (input) {
            is CompoundInput -> {
                val currentState = input.getComponent<StatefulInput<S>>().state
                ParseSuccess(stateful(input, body(Option.just(currentState))), Unit)
            }
            else -> {
                ParseSuccess(stateful(input, body(Option.empty())), Unit)
            }
        }
    }
