package at.searles.parsingtools.generator

import at.searles.parsing.Trace

abstract class VisitorNode(val trace: Trace) {
    abstract fun <A> accept(visitor: Visitor<A>): A
}