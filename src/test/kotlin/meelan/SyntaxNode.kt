package meelan

import at.searles.parsing.utils.ast.AstNode
import at.searles.parsing.utils.ast.SourceInfo

abstract class SyntaxNode(sourceInfo: SourceInfo): AstNode(sourceInfo) {
}