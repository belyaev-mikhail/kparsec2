package ru.spbstu

import ru.spbstu.wheels.hashCombine
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.reflect.KProperty

interface InputComponent

@OptIn(ExperimentalStdlibApi::class)
class CompoundInput<T>(val base: Input<T>,
                       val components: Map<KType, InputComponent> = emptyMap()): Input<T>(base.source) {
    override val location: Location<*>
        get() = base.location
    override fun advance(): Input<T> = CompoundInput(base.advance(), components)
    override fun drop(n: Int): Input<T> = CompoundInput(base.drop(n), components)

    inline fun <reified T: InputComponent> putComponent(value: T) =
        CompoundInput(base, components + (typeOf<T>() to value))

    inline fun <reified T: InputComponent> getComponent() = components[typeOf<T>()] as T
    inline fun <reified T: InputComponent> putComponentIfAbsent(body: () -> T) = when {
        typeOf<T>() !in components -> putComponent(body())
        else -> this
    }

    inline operator fun <reified T: InputComponent> getValue(thisRef: Any?, prop: KProperty<*>): T =
        getComponent()

    override fun equals(other: Any?): Boolean = when (other) {
        is CompoundInput<*> -> base == other.base && components == other.components
        else -> false
    }

    override fun hashCode(): Int = hashCombine(base, components)

    override fun toString(): String = when {
        components.isEmpty() -> "$base"
        else -> "$base with $components"
    }
}

inline fun <T, reified C: InputComponent> Input<T>.putComponentIfAbsent(body: () -> C) = when {
    this !is CompoundInput -> CompoundInput(this).putComponentIfAbsent(body)
    else -> (this as CompoundInput<T>).putComponentIfAbsent(body)
}
