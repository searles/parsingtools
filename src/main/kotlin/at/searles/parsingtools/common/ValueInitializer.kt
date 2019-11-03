package at.searles.parsingtools.common

import at.searles.parsing.Initializer
import at.searles.parsing.ParserStream

/**
 * Created by searles on 02.04.19.
 */
class ValueInitializer<V>(private val value: V) : Initializer<V> {

    override fun parse(stream: ParserStream): V? {
        return value
    }

    override fun consume(v: V): Boolean {
        return v == value
    }

    override fun toString(): String {
        return String.format("{%s}", value)
    }
}
