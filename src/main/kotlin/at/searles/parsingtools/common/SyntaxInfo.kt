package at.searles.parsingtools.common

import at.searles.parsing.ParserStream

open class SyntaxInfo {

    val start: Long
    val end: Long

    protected constructor(stream: ParserStream) {
        this.start = stream.start
        this.end = stream.end
    }

    protected constructor(node: SyntaxInfo) {
        this.start = node.start
        this.end = node.end
    }

    override fun toString(): String {
        return String.format("[%d:%d]", start, end)
    }
}
