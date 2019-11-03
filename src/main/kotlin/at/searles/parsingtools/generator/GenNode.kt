package at.searles.parsingtools.generator

import at.searles.parsing.ParserStream
import at.searles.parsingtools.common.SyntaxInfo

abstract class GenNode(stream: ParserStream): SyntaxInfo(stream) {
    abstract fun <A> accept(visitor: Visitor<A>): A
}