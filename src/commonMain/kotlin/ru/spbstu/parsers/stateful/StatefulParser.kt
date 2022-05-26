package ru.spbstu.parsers.stateful

import ru.spbstu.*
import ru.spbstu.parsers.combinators.DoScope
import ru.spbstu.parsers.combinators.filter
import ru.spbstu.parsers.combinators.namedParser
import ru.spbstu.wheels.Option
import ru.spbstu.wheels.map
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*

inline fun <reified S> stateGet(): Parser<Any?, S> = namedParser("stateGet") { input ->
    when (input) {
        is CompoundInput -> input.success(input[StatefulInput.Key<S>()].state)
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
                val currentState = input[StatefulInput.Key<S>()].state
                ParseSuccess(stateful(input, body(currentState)), Unit)
            }
            else -> throw IllegalStateException("stateGet() called on non-stateful input")
        }
    }

inline fun <reified S> stateModifyIfSet(crossinline body: (Option<S>) -> S): Parser<Any?, Unit> =
    namedParser("stateModifyIfSet(..)") { input ->
        val key = StatefulInput.Key<S>()
        val currentState = Option.ofNullable(input.getComponentOrNull(key)).map { it.state }
        stateful(input, body(currentState)).unitSuccess()
    }

inline fun <reified S> stateRequire(crossinline predicate: (S) -> Boolean): Parser<Any?, S> =
    stateGet<S>().filter("stateRequire", predicate)

inline fun <T, reified S> DoScope<T>.parserState() = object : ReadWriteProperty<Any?, S> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): S = parse(stateGet<S>())

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: S) {
        parse(stateSet(value))
    }
}
