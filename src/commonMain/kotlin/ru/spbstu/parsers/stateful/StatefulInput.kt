package ru.spbstu.parsers.stateful

import ru.spbstu.CompoundInput
import ru.spbstu.Input
import ru.spbstu.InputComponent
import ru.spbstu.putComponentIfAbsent

class StatefulInput<S>(val state: S): InputComponent

inline fun <T, reified S> stateful(input: Input<T>, state: S): CompoundInput<T> =
    input.putComponentIfAbsent { StatefulInput(state) }
