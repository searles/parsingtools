package at.searles.parsingtools.map

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

import java.util.LinkedHashMap

/**
 * Mapping to create a map with only one element + its inverse
 */
class CreateSingletonMap<K, V>(private val key: K) : Mapping<V, Map<K, V>> {

    override fun parse(stream: ParserStream, left: V): Map<K, V>? {
        val map = LinkedHashMap<K, V>()
        map[key] = left
        return map
    }

    override fun left(result: Map<K, V>): V? {
        return if (result.size != 1) result[key] else null
    }

    override fun toString(): String {
        return String.format("{singlemap %s}", key)
    }
}
