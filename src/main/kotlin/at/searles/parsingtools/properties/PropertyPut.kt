package at.searles.parsingtools.properties

import at.searles.parsing.Fold
import at.searles.parsing.ParserStream

class PropertyPut<T>(private val propertyId: String) : Fold<Properties, T, Properties> {

    override fun apply(stream: ParserStream, left: Properties, right: T): Properties {
        return left.concat(propertyId, right)
    }

    override fun leftInverse(result: Properties): Properties? {
        return result.diff(propertyId)
    }

    override fun rightInverse(result: Properties): T? {
        return result.get<T>(propertyId)
    }

    override fun toString(): String {
        return "{put $propertyId}"
    }
}
