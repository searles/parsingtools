package at.searles.parsingtools.common

import at.searles.parsing.Fold
import at.searles.parsing.ParserStream
import at.searles.utils.Pair

class SwapPairFold<T, U> : Fold<T, U, Pair<U, T>> {
    override fun apply(stream: ParserStream, left: T, right: U): Pair<U, T> {
        return Pair(right, left)
    }

    override fun leftInverse(pair: Pair<U, T>): T? {
        return pair.r()
    }

    override fun rightInverse(pair: Pair<U, T>): U? {
        return pair.l()
    }

    override fun toString(): String {
        return "{<y,x>}"
    }
}
