package ru.spbstu.kparsec2

import kotlin.jvm.JvmInline

fun interface Location<in T> {
    operator fun invoke(token: T): Location<T>

    val canSkip: Boolean get() = false
    fun skip(tokens: Int): Location<T> = error("${this::class} cannot skip characters")
}

object NoLocation: Location<Any?> {
    override fun invoke(token: Any?): NoLocation = this
    override fun toString(): String = "<unknown>"
    override val canSkip: Boolean get() = true

    override fun skip(tokens: Int): NoLocation = this
}

data class CharLocation(val line: Int = 1, val col: Int = 0): Location<Char> {
    override fun invoke(token: Char): CharLocation = when {
        run { token } == '\n' -> { // without run {} this triggers a strange bug, resulting in false in js-legacy-node
            copy(line = line + 1, col = 0)
        }
        else -> copy(col = col + 1)
    }

    override fun toString(): String = "$line:$col"
}

@JvmInline
value class OffsetLocation<T>(val offset: Int = 0): Location<T> {
    override fun invoke(token: T): OffsetLocation<T> = OffsetLocation(offset + 1)
    override fun toString(): String = "$offset"

    override val canSkip: Boolean
        get() = true

    override fun skip(tokens: Int): OffsetLocation<T> = OffsetLocation(offset + tokens)
}

interface LocationManager<T> {
    fun start(): Location<T>
    fun resolve(loc: Location<T>): Location<T>
}

class CharLocationManager(val source: Source<Char>): LocationManager<Char> {
    inner class ManagedLocation(val offset: Int): Location<Char> {
        override fun invoke(token: Char) = ManagedLocation(offset + 1)
        override val canSkip: Boolean
            get() = true
        override fun skip(tokens: Int): ManagedLocation = ManagedLocation(offset + tokens)

        override fun equals(other: Any?): Boolean = other is ManagedLocation && other.offset == offset
        override fun hashCode(): Int = offset.hashCode()
        override fun toString(): String = resolve(this).toString()
    }

    private val locationCache: MutableMap<Int, CharLocation> = mutableMapOf()

    private fun resolve(loc: Int): CharLocation {
        var cur = CharLocation()
        var offset = 0
        for (t in source) {
            cur = cur(t)
            ++offset
            if (offset >= loc) break
        }
        return cur
    }

    override fun start(): Location<Char> = ManagedLocation(0)

    override fun resolve(loc: Location<Char>): Location<Char> = when(loc) {
        is ManagedLocation -> locationCache.getOrPut(loc.offset) { resolve(loc.offset) }
        else -> loc
    }
}

fun managedCharLocation(source: Source<Char>,
                        locationManager: LocationManager<Char> = CharLocationManager(source)
) =
    locationManager.start()

