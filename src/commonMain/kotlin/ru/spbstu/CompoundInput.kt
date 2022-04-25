package ru.spbstu

import ru.spbstu.wheels.hashCombine
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.reflect.KProperty

interface InputComponent {
    interface Key<T: InputComponent> {
        val type: KType
    }

    companion object {
        abstract class AbstractKey<T: InputComponent>: Key<T> {
            abstract override val type: KType
            override fun equals(other: Any?): Boolean = other is Key<*> && type == other.type
            override fun hashCode(): Int = type.hashCode()
            override fun toString(): String = "Key<$type>"
        }
        @PublishedApi
        internal data class SimpleKey<T: InputComponent>(override val type: KType): AbstractKey<T>()
        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified T: InputComponent> Key(): Key<T> = SimpleKey(typeOf<T>())
    }
}

@OptIn(ExperimentalStdlibApi::class)
class CompoundInput<T>(val base: Input<T>,
                       val components: Map<KType, InputComponent> = emptyMap()): Input<T>(base.source) {
    override val location: Location<*>
        get() = base.location
    override fun advance(): CompoundInput<T> = CompoundInput(base.advance(), components)
    override fun drop(n: Int): CompoundInput<T> = CompoundInput(base.drop(n), components)

    inline fun <reified T: InputComponent> putComponent(value: T) =
        CompoundInput(base, components + (typeOf<T>() to value))

    inline fun <reified T: InputComponent> getComponent() = components[typeOf<T>()] as T
    inline fun <reified T: InputComponent> putComponentIfAbsent(body: () -> T) = when {
        typeOf<T>() !in components -> putComponent(body())
        else -> this
    }
    inline operator fun <reified T: InputComponent> getValue(thisRef: Any?, prop: KProperty<*>): T =
        getComponent()

    inline fun <T: InputComponent> getComponentOrNull(key: InputComponent.Key<T>): T? =
        components[key.type] as T?

    operator fun <T: InputComponent> get(key: InputComponent.Key<T>) =
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

inline fun <T, C: InputComponent> Input<T>.getComponentOrNull(key: InputComponent.Key<C>) =
    when(this) {
        !is CompoundInput -> null
        else -> getComponentOrNull(key)
    }

inline fun <T, reified C: InputComponent> Input<T>.putComponentIfAbsent(body: () -> C) = when {
    this !is CompoundInput -> CompoundInput(this).putComponentIfAbsent(body)
    else -> (this as CompoundInput<T>).putComponentIfAbsent(body)
}

fun<T> compound(input: Input<T>): CompoundInput<T> = when(input) {
    is CompoundInput -> input
    else -> CompoundInput(input)
}
