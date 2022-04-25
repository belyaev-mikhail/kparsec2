package ru.spbstu.parsers.stateful

import ru.spbstu.*

class StatefulInput<S>(val state: S): InputComponent {
    companion object {
        inline fun <reified S> Key(): InputComponent.Key<StatefulInput<S>> = InputComponent.Key()
    }

    override fun toString(): String = "State($state)"
}

inline fun <T, reified S> stateful(input: Input<T>, state: S): CompoundInput<T> =
    compound(input).putComponent(StatefulInput(state))
