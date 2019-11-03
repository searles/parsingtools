package at.searles.parsingtools.common

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

class ToString : Mapping<CharSequence, String> {

    override fun parse(stream: ParserStream, left: CharSequence): String? {
        return left.toString()
    }

    override fun left(result: String): CharSequence? {
        return result
    }

    private object Holder {
        internal val INSTANCE = ToString()
    }

    companion object {

        val instance: ToString
            get() = Holder.INSTANCE
    }
}
