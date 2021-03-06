package ru.spbstu

import ru.spbstu.kparsec2.CharLocationType
import ru.spbstu.kparsec2.SimpleInput
import ru.spbstu.kparsec2.Source
import ru.spbstu.wheels.collections.asCharSequence
import java.io.*
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.nio.CharBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

data class WeakLazy<T: Any>(val compute: () -> T, var ref: WeakReference<T>? = null): Lazy<T> {
    private fun create(): T {
        val obj = compute()
        this.ref = WeakReference(obj)
        return obj
    }

    fun get(): T {
        val ref = ref ?: return create()
        return ref.get() ?: return create()
    }

    override val value: T
        get() = get()

    override fun isInitialized(): Boolean = when (val ref = ref) {
        null -> false
        else -> ref.get() != null
    }
}

data class SoftLazy<T: Any>(val compute: () -> T, var ref: SoftReference<T>? = null) {
    private fun create(): T {
        val obj = compute()
        this.ref = SoftReference(obj)
        return obj
    }

    fun get(): T {
        val ref = ref ?: return create()
        return ref.get() ?: return create()
    }
}

private class MemoCell<out T> private constructor(val value: T, var next: Any?) {
    constructor(value: T, next: () -> MemoCell<T>?): this(value, next as Any?)

    fun getNextCell(): MemoCell<T>? = when(val next = next) {
        null -> null
        is MemoCell<*> -> next as MemoCell<T>
        is Function0<*> -> {
            this.next = next.invoke()
            getNextCell()
        }
        else -> throw IllegalStateException()
    }

    companion object {
        val LAST = MemoCell<Any?>(null, null)
    }
}

private fun <T> memoCell(nextFunction: () -> T?): MemoCell<T>? {
    val current = nextFunction() ?: return null
    return MemoCell(current) { memoCell(nextFunction) }
}

private fun fileToCell(path: Path, encoding: Charset, bufferSize: Int): MemoCell<CharSequence>? =
    readerToCell(Files.newBufferedReader(path, encoding), bufferSize, closeOnFinish = true)

private fun readerToCell(reader: Reader, bufferSize: Int,
                         closeOnFinish: Boolean): MemoCell<CharSequence>? {
    return memoCell {
        val buf = CharArray(bufferSize)
        val read = reader.read(buf)
        if (read == -1) null.also { if (closeOnFinish) reader.close() }
        else buf.asCharSequence(0, read)
    }
}

class FileSource
private constructor(
    private val cells: MemoCell<CharSequence>?,
    private val offset: Int
): Source<Char> {
    constructor(path: Path, encoding: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE):
        this(fileToCell(path, encoding, bufferSize), 0)
    constructor(reader: Reader, bufferSize: Int = DEFAULT_BUFFER_SIZE, closeOnFinish: Boolean = true):
        this(readerToCell(reader, bufferSize, closeOnFinish), 0)

    private fun copy(cells: MemoCell<CharSequence>? = this.cells, offset: Int = this.offset) =
        FileSource(cells, offset)

    private fun nextCellSource(): FileSource = copy(cells?.getNextCell(), 0)

    private val currentBuffer: CharSequence
        get() = cells!!.value

    override val current: Char
        get() = currentBuffer[offset]

    override fun hasNext(): Boolean = cells != null

    override fun advance(): Source<Char> {
        cells ?: return this

        return when {
            offset < currentBuffer.lastIndex -> copy(offset = offset + 1)
            else -> nextCellSource()
        }
    }

    override fun drop(n: Int): Source<Char> {
        cells ?: return this

        return when {
            n == 0 -> this
            offset + n < currentBuffer.length -> copy(offset = offset + n)
            else -> {
                val adjustedN = (offset + n) - currentBuffer.length
                nextCellSource().drop(adjustedN)
            }
        }
    }

    override fun toString(): String = when {
        !hasNext() -> ""
        else -> "${currentBuffer.drop(offset)}..."
    }

    companion object {
        const val DEFAULT_BUFFER_SIZE = 0xffff
    }

}

fun fileInput(file: Path, encoding: Charset = Charsets.UTF_8,
              locationType: CharLocationType = CharLocationType.LINES,
              bufferSize: Int = FileSource.DEFAULT_BUFFER_SIZE) =
    when(val source = FileSource(file, encoding, bufferSize)) {
        else -> SimpleInput(source, locationType.start(source))
    }

fun fileInput(file: String, encoding: Charset = Charsets.UTF_8,
              locationType: CharLocationType = CharLocationType.LINES,
              bufferSize: Int = FileSource.DEFAULT_BUFFER_SIZE) =
    fileInput(Path(file), encoding, locationType, bufferSize)

fun fileInput(file: File, encoding: Charset = Charsets.UTF_8,
              locationType: CharLocationType = CharLocationType.LINES,
              bufferSize: Int = FileSource.DEFAULT_BUFFER_SIZE) =
    fileInput(file.toPath(), encoding, locationType, bufferSize)

fun fileInput(stream: InputStream,
              encoding: Charset = Charsets.UTF_8,
              locationType: CharLocationType = CharLocationType.LINES,
              bufferSize: Int = FileSource.DEFAULT_BUFFER_SIZE,
              closeOnFinish: Boolean = false) =
    when(val source = FileSource(stream.reader(encoding), bufferSize, closeOnFinish)) {
        else -> SimpleInput(source, locationType.start(source))
    }

fun fileInput(reader: Reader,
              locationType: CharLocationType = CharLocationType.LINES,
              bufferSize: Int = FileSource.DEFAULT_BUFFER_SIZE,
              closeOnFinish: Boolean = false) =
    when(val source = FileSource(reader, bufferSize, closeOnFinish)) {
        else -> SimpleInput(source, locationType.start(source))
    }

fun fileInput(channel: ReadableByteChannel,
              encoding: Charset = Charsets.UTF_8,
              locationType: CharLocationType = CharLocationType.LINES,
              bufferSize: Int = FileSource.DEFAULT_BUFFER_SIZE,
              closeOnFinish: Boolean = false) =
    when(val source = FileSource(Channels.newReader(channel, encoding), bufferSize, closeOnFinish)) {
        else -> SimpleInput(source, locationType.start(source))
    }
