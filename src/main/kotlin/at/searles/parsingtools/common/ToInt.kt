package at.searles.parsingtools.common

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

class ToInt : Mapping<CharSequence, Int> {
    override fun parse(stream: ParserStream, left: CharSequence): Int? {
        try {
            return Integer.parseInt(left.toString())
        } catch (e: NumberFormatException) {
            return null
        }

    }

    override fun left(result: Int): CharSequence? {
        return result.toString()
    }
}
