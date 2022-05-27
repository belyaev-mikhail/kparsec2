package ru.spbstu.kparsec2.parsers.stateful

import ru.spbstu.kparsec2.CompoundInput
import ru.spbstu.kparsec2.Input
import ru.spbstu.kparsec2.InputComponent
import ru.spbstu.kparsec2.compound

class StatefulInput<S>(val state: S): InputComponent<StatefulInput<S>> {
    companion object {
        inline fun <reified S> Key(): InputComponent.Key<StatefulInput<S>> = InputComponent.Key()
    }

    override fun toString(): String = "State($state)"
}

inline fun <T, reified S> stateful(input: Input<T>, state: S): CompoundInput<T> =
    compound(input).putComponent(StatefulInput(state))
