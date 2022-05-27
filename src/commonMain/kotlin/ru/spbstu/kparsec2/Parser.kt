package ru.spbstu.kparsec2

fun interface Parser<in T, out R> {
    operator fun invoke(input: Input<T>): ParseResult<@UnsafeVariance T, R>

    companion object
}

operator fun <R> Parser<Char, R>.invoke(input: CharSequence): ParseResult<Char, R> = invoke(stringInput(input))

fun <T, R> parser(body: () -> Parser<T, R>): Parser<T, R> = Parser { input ->
    val delegate = body()
    delegate(input)
}
