package at.searles.parsingtools.common

import at.searles.parsing.Initializer
import at.searles.parsing.ParserStream

/**
 * Initializer that introduces a simple number
 */
class Num(private val num: Int) : Initializer<Int> {

    override fun parse(stream: ParserStream): Int? {
        return num
    }

    override fun consume(integer: Int?): Boolean {
        return integer == num
    }

    override fun toString(): String {
        return String.format("{num: %d}", num)
    }
}
