package at.searles.parsing.tools.generator

import at.searles.regexparser.EscStringParser
import java.lang.IllegalArgumentException

class KotlinVisitor: Visitor<String> {

    private var isRegex = false

    val genericHeader = """
import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.Mapping
import at.searles.parsing.Parser
import at.searles.parsing.Reducer
import at.searles.parsing.Ref
import at.searles.parsing.tools.generator.Context
import at.searles.regex.CharSet
import at.searles.regex.Regex""".trimIndent()

    val programHeader = """
    private val tokenizer = SkipTokenizer(Lexer())
    private val context = Context(tokenizer)
    """

    override fun visit(node: Generator.Program): String {
        return "${node.header.trimIndent()}\n" +
                "\n" +
                "${genericHeader}\n" +
                "\n" +
                "${node.grammar.accept(this)}\n"
    }

    override fun visit(node: Generator.Grammar): String {
        val writer = StringBuilder()
        writer.append("object ${node.name} {\n" +
                "$programHeader\n" +
                "\n")

        val predefinedRefs = node.content.filterIsInstance<Generator.ParserRuleNode>().map{it.lhs}.filter{it.type != null}

        // typed parser rules are predefined as refs.
        predefinedRefs.forEach {
            writer.append("    val ${it.name} = Ref<${it.type!!.accept(this)}>(\"${it.name}\")\n")
        }

        node.content.forEach {
            writer.append("    // position ${it.start}-${it.end}\n")
            writer.append(it.accept(this))
            writer.append("\n\n")
        }

        writer.append("}")

        return writer.toString()

    }

    override fun visit(node: Generator.ParserRuleNode): String {
        if(node.lhs.type == null) {
            return "    val ${node.lhs.name} = ${node.rhs.accept(this)}"
        }

        // typed ones are defined before as Refs
        return "    init {\n        ${node.lhs.name}.set(${node.rhs.accept(this)})\n    }"

    }

    override fun visit(node: Generator.RegexRuleNode): String {
        isRegex = true
        val regexString = node.rhs.accept(this)
        isRegex = false

        return "    val ${node.lhs} = context.parser($regexString)"
    }

    override fun visit(node: Generator.FragmentRuleNode): String {
        isRegex = true
        val regexString = node.rhs.accept(this)
        isRegex = false

        return "    val ${node.lhs}: Regex = $regexString"
    }

    override fun visit(node: Generator.UnionNode): String {
        return "${node.left.accept(this)}.or(${node.right.accept(this)})"
    }

    override fun visit(node: Generator.ConcatNode): String {
        return "${node.left.accept(this)}.then(${node.right.accept(this)})"
    }

    override fun visit(node: Generator.AnnotationNode): String {
        if(isRegex) {
            handleError("annotations not allowed in regex", node)
        }

        return "${node.expr.accept(this)}.annotate(${node.annotation.accept(this)})"
    }

    override fun visit(node: Generator.FoldNode): String {
        if(isRegex) {
            handleError("fold not allowed in regex", node)
        }

        return "${node.expr.accept(this)}.fold(${node.fold.accept(this)})"
    }

    override fun visit(node: Generator.Opt): String {
        return "${node.expr.accept(this)}.opt()"
    }

    override fun visit(node: Generator.Plus): String {
        return "${node.expr.accept(this)}.plus()"
    }

    override fun visit(node: Generator.Stop): String {
        if(!isRegex) {
            handleError("non-greedy-op only allowed in regex", node)
        }
        
        return "${node.expr.accept(this)}.nonGreedy()"
    }

    override fun visit(node: Generator.RangeNode): String {
        if(!isRegex) {
            handleError("range is only allowed in regexes", node)
        }

        val exprString = node.expr.accept(this)

        return when {
            (node.range.lo == node.range.hi) -> "$exprString.count(${node.range.lo})"
            (node.range.hi < 0) -> "${exprString}.min(${node.range.lo})"
            else -> "$exprString.range(${node.range.lo}, ${node.range.hi})"
        }
    }

    override fun visit(node: Generator.Star): String {
        return "${node.expr.accept(this)}.rep()"
    }

    override fun visit(node: Generator.CodeNode): String {
        return node.code
    }

    override fun visit(node: Generator.CharSetNode): String {
        if(!isRegex) {
            handleError("character sets are only allowed in regexes", node)
        }

        val intervals = node.set.map { "${it.start}, ${it.end-1}" }.joinToString(", ")
        return "CharSet.interval($intervals)"
    }

    override fun visit(node: Generator.RegexParserNode): String {
        if(isRegex) {
            handleError("already in a regex", node)
        }

        isRegex = true
        val regexString = node.regex.accept(this)
        isRegex = false

        val fnString = node.fn.accept(this)

        return "context.parser($regexString, $fnString)"
    }

    private fun handleError(msg: String, node: GenNode) {
        throw IllegalArgumentException("$msg at $node")
    }

    override fun visit(node: Generator.IdNode): String {
        return node.id
    }

    override fun visit(node: Generator.StringNode): String {
        // FIXME: Use dedicated java-string-parser
        val javaString = EscStringParser.unparse(node.string)

        return if(isRegex) {
             "Regex.text(\"$javaString\")"
        } else {
            "context.text(\"$javaString\")"
        }
    }

}