package at.searles.parsingtools.properties

import at.searles.parsing.Initializer
import at.searles.parsing.ParserStream

class PropertiesInitializer : Initializer<Properties> {

    private object Holder {
        internal var instance = Properties()
    }

    override fun parse(stream: ParserStream): Properties? {
        return Holder.instance
    }

    override fun consume(properties: Properties): Boolean {
        return properties.isEmpty()
    }

    override fun toString(): String {
        return "{empty properties}"
    }
}