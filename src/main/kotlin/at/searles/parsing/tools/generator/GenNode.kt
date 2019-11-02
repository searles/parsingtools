package at.searles.parsing.tools.generator

import at.searles.parsing.ParserStream
import at.searles.parsing.tools.common.SyntaxInfo

abstract class GenNode(stream: ParserStream): SyntaxInfo(stream) {
    abstract fun <A> accept(visitor: Visitor<A>): A
}