package at.searles.parsingtools.properties

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

import java.util.HashMap

class PropertiesSingleton<T>(private val id: String) : Mapping<T, Properties> {

    override fun parse(stream: ParserStream, left: T): Properties? {
        val map = HashMap<String, Any>()
        map.put(id, left!!)
        return Properties(map)
    }

    override fun left(result: Properties): T? {
        @Suppress("UNCHECKED_CAST")
        return if (result.size() != 1) {
            null
        } else result.get<Any>(id) as T?

    }
}
