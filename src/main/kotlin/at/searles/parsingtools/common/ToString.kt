package at.searles.parsingtools.common

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

object ToString : Mapping<CharSequence, String> {
    override fun parse(stream: ParserStream, left: CharSequence): String? {
        return left.toString()
    }

    override fun left(result: String): CharSequence? {
        return result
    }
}
