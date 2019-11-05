package at.searles.parsingtools.common

import at.searles.parsing.Fold
import at.searles.parsing.ParserStream
import at.searles.utils.Pair

/**
 * Created by searles on 02.04.19.
 */
class CreatePair<T, U> : Fold<T, U, Pair<T, U>> {
    override fun apply(stream: ParserStream, left: T, right: U): Pair<T, U> {
        return Pair(left, right)
    }

    override fun leftInverse(pair: Pair<T, U>): T? {
        return pair.l()
    }

    override fun rightInverse(pair: Pair<T, U>): U? {
        return pair.r()
    }

    override fun toString(): String {
        return "{<x,y>}"
    }
}
