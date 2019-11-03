package at.searles.parsingtools.map

import at.searles.parsing.Initializer
import at.searles.parsing.ParserStream
import java.util.HashMap

/**
 * Created by searles on 31.03.19.
 */
class EmptyMap<K, V> : Initializer<Map<K, V>> {

    override fun parse(stream: ParserStream): Map<K, V>? {
        return HashMap()
    }

    override fun consume(map: Map<K, V>): Boolean {
        return map.isEmpty()
    }

    override fun toString(): String {
        return "{emptymap}"
    }
}
