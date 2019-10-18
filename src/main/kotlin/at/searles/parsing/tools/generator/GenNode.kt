package at.searles.parsing.tools.generator

import at.searles.parsing.utils.ast.AstNode
import at.searles.parsing.utils.ast.SourceInfo

abstract class GenNode(info: SourceInfo): AstNode(info) {
    open fun toCode(): String = errorMsg(sourceInfo())
    open fun toRegex(): String = errorMsg(sourceInfo())

    private fun errorMsg(sourceInfo: SourceInfo): String {
        return "/* ERROR $sourceInfo */"
    }
}