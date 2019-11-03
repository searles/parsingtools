package at.searles.parsingtools.map

import at.searles.parsing.Fold
import at.searles.parsing.ParserStream

import java.util.LinkedHashMap

/**
 * Created by searles on 31.03.19.
 */
class PutFold<K, V>(private val key: K) : Fold<Map<K, V>, V, Map<K, V>> {

    override fun apply(stream: ParserStream, left: Map<K, V>, right: V): Map<K, V> {
        val map = LinkedHashMap(left)
        map[key] = right
        return map
    }

    override fun leftInverse(result: Map<K, V>): Map<K, V>? {
        if (!result.containsKey(key)) {
            return null
        }

        val left = LinkedHashMap(result)
        left.remove(key)
        return left
    }

    override fun rightInverse(result: Map<K, V>): V? {
        return if (!result.containsKey(key)) {
            null
        } else result[key]

    }

    override fun toString(): String {
        return String.format("{put %s}", key)
    }
}
