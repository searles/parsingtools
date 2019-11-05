package at.searles.parsingtools.generator

import at.searles.parsing.ParserStream
import at.searles.parsingtools.SyntaxInfo

abstract class VisitorNode(stream: ParserStream): SyntaxInfo(stream) {
    abstract fun <A> accept(visitor: Visitor<A>): A
}