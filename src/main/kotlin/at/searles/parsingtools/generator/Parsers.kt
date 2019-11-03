package at.searles.parsingtools.generator

import at.searles.parsing.Reducer

fun <T> Reducer<T, T>.opt(): Reducer<T, T> {
    return Reducer.opt(this)
}

fun <T> Reducer<T, T>.rep(): Reducer<T, T> {
    return Reducer.rep(this)
}

fun <T> Reducer<T, T>.plus(): Reducer<T, T> {
    return Reducer.rep(this)
}

