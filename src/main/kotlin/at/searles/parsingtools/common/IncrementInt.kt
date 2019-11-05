package at.searles.parsingtools.common

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

class IncrementInt(private val min: Int) : Mapping<Int, Int> {

    override fun parse(stream: ParserStream, left: Int): Int? {
        return left + 1
    }

    override fun left(result: Int): Int? {
        return if (result > min) result - 1 else null
    }

    override fun toString(): String {
        return String.format("{%d...+1}", min)
    }
}
