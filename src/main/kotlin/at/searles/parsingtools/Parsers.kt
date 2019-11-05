package at.searles.parsingtools

import at.searles.parsing.Parser
import at.searles.parsing.Recognizer
import at.searles.parsing.Reducer
import at.searles.parsingtools.list.AddToList
import at.searles.parsingtools.list.CreateEmptyList
import at.searles.parsingtools.list.CreateSingletonList
import at.searles.parsingtools.opt.CreateNone
import at.searles.parsingtools.opt.CreateSome
import java.util.*

fun <T> Reducer<T, T>.opt(): Reducer<T, T> {
    return Reducer.opt(this)
}

fun <T> Reducer<T, T>.rep(): Reducer<T, T> {
    return Reducer.rep(this)
}

fun <T> Reducer<T, T>.plus(): Reducer<T, T> {
    return Reducer.rep(this)
}

/**
 * Parser for a separated list.
 *
 * @param <T>       The base type.
 * @param separator The separator, eg a comma
 * @param parser    The parser for all elements
 * @return An invertible parser for a list of items.
</T> */
fun <T> Parser<T>.list1(separator: Recognizer): Parser<List<T>> {
    // XXX there is also joinPlus!
    return this.then(CreateSingletonList()).then(
        separator.then(this.fold(AddToList(1))).rep()
    )
}

fun <T> Parser<T>.list1(): Parser<List<T>> {
    // XXX there is also joinPlus.
    return this.then(CreateSingletonList()).then(
        this.fold(AddToList(1)).rep()
    )
}

fun <T> Parser<T>.list(separator: Recognizer): Parser<List<T>> {
    return CreateEmptyList<T>().then(separator.join(this.fold(AddToList(0))))
}

fun <T> Parser<T>.list(): Parser<List<T>> {
    return CreateEmptyList<T>().then(this.fold(AddToList(0)).rep())
}

fun <T> Parser<T>.optional(): Parser<Optional<T>> {
    return this.then(CreateSome()).or(CreateNone())
}
