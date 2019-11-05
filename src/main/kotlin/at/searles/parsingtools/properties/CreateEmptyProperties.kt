package at.searles.parsingtools.properties

import at.searles.parsing.Initializer
import at.searles.parsing.ParserStream

object CreateEmptyProperties : Initializer<Properties> {

    private val emptyProperties = Properties()
    
    override fun parse(stream: ParserStream): Properties? {
        return emptyProperties
    }

    override fun consume(properties: Properties): Boolean {
        return properties.isEmpty()
    }

    override fun toString(): String {
        return "{empty properties}"
    }
}