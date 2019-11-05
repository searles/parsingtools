package at.searles.parsingtools.list

import at.searles.parsing.Initializer
import at.searles.parsing.ParserStream
import java.util.ArrayList
import java.util.Collections

/**
 * Initializer that introduces an empty list
 */
class CreateEmptyList<T> : Initializer<List<T>> {
    private object Holder {
        internal var instance: List<*> = emptyList<Any>()
    }

    override fun parse(stream: ParserStream): List<T>? {
        @Suppress("UNCHECKED_CAST")
        return Holder.instance as List<T>
    }

    override fun consume(ts: List<T>): Boolean {
        return ts.isEmpty()
    }

    override fun toString(): String {
        return "{emptylist}"
    }
}
