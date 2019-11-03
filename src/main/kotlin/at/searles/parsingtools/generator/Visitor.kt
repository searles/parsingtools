package at.searles.parsingtools.generator

interface Visitor<A> {
    fun visit(node: Generator.RegexParserNode): A
    fun visit(node: Generator.IdNode): A
    fun visit(node: Generator.StringNode): A
    fun visit(node: Generator.CodeNode): A
    fun visit(node: Generator.CharSetNode): A
    fun visit(node: Generator.Star): A
    fun visit(node: Generator.Opt): A
    fun visit(node: Generator.Plus): A
    fun visit(node: Generator.Stop): A
    fun visit(node: Generator.RangeNode): A
    fun visit(node: Generator.FoldNode): A
    fun visit(node: Generator.AnnotationNode): A
    fun visit(node: Generator.ConcatNode): A
    fun visit(node: Generator.UnionNode): A
    fun visit(node: Generator.FragmentRuleNode): A
    fun visit(node: Generator.RegexRuleNode): A
    fun visit(node: Generator.ParserRuleNode): A
    fun visit(node: Generator.Program): A
    fun visit(node: Generator.Grammar): A
}