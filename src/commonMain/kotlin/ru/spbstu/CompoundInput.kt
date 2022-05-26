package ru.spbstu

import kotlinx.warnings.Warnings
import ru.spbstu.wheels.hashCombine
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.reflect.KProperty

interface InputComponent<Self: InputComponent<Self>> {
    interface Key<T: InputComponent<T>> {
        val type: KType
    }

    fun advance(n: Int): InputComponent<Self> = this

    companion object {
        abstract class AbstractKey<T: InputComponent<T>>: Key<T> {
            abstract override val type: KType
            override fun equals(other: Any?): Boolean = other is Key<*> && type == other.type
            override fun hashCode(): Int = type.hashCode()
            override fun toString(): String = "Key<$type>"
        }
        @PublishedApi
        internal data class SimpleKey<T: InputComponent<T>>(override val type: KType): AbstractKey<T>()
        inline fun <reified T: InputComponent<T>> Key(): Key<T> = SimpleKey(typeOf<T>())
    }
}

class CompoundInput<T>(val base: Input<T>,
                       val components: Map<KType, InputComponent<*>> = emptyMap()): Input<T>(base.source) {
    override val location: Location<*>
        get() = base.location
    override fun advance(): CompoundInput<T> =
        CompoundInput(base.advance(), components.mapValues { (_, v) -> v.advance(1) })
    override fun drop(n: Int): CompoundInput<T> =
        CompoundInput(base.drop(n), components.mapValues { (_, v) -> v.advance(n) })

    inline fun <reified T: InputComponent<T>> putComponent(value: T) =
        CompoundInput(base, components + (typeOf<T>() to value))

    inline fun <reified T: InputComponent<T>> getComponent() = components[typeOf<T>()] as T
    inline fun <reified T: InputComponent<T>> putComponentIfAbsent(body: () -> T) = when {
        typeOf<T>() !in components -> putComponent(body())
        else -> this
    }
    inline operator fun <reified T: InputComponent<T>> getValue(thisRef: Any?, prop: KProperty<*>): T =
        getComponent()

    inline fun <T: InputComponent<T>> getComponentOrNull(key: InputComponent.Key<T>): T? =
        components[key.type] as T?

    operator fun <T: InputComponent<T>> get(key: InputComponent.Key<T>) =
        components[key.type] as T

    override fun equals(other: Any?): Boolean = when (other) {
        is CompoundInput<*> -> base == other.base && components == other.components
        else -> false
    }

    override fun hashCode(): Int = hashCombine(base, components)

    override fun toString(): String = when {
        components.isEmpty() -> "$base"
        else -> "$base with ${components.values}"
    }
}

inline fun <T, C: InputComponent<C>> Input<T>.getComponentOrNull(key: InputComponent.Key<C>) =
    when(this) {
        !is CompoundInput -> null
        else -> getComponentOrNull(key)
    }

inline fun <T, reified C: InputComponent<C>> Input<T>.putComponentIfAbsent(body: () -> C) = when {
    this !is CompoundInput -> CompoundInput(this).putComponentIfAbsent(body)
    else -> (this as CompoundInput<T>).putComponentIfAbsent(body)
}

fun<T> compound(input: Input<T>): CompoundInput<T> = when(input) {
    is CompoundInput -> input
    else -> CompoundInput(input)
}
