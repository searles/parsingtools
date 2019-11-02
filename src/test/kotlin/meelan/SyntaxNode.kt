package meelan

import at.searles.parsing.tools.common.SyntaxInfo
import at.searles.parsing.ParserStream

abstract class SyntaxNode(stream: ParserStream): SyntaxInfo(stream) {
}