package at.searles.parsing.tools.generator

import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.*
import at.searles.parsing.utils.ast.SourceInfo
import at.searles.parsing.utils.list.Append
import at.searles.regex.CharSet
import at.searles.regex.RegexParser
import at.searles.regex.Regex
import at.searles.parsing.utils.list.EmptyList

/* ---------------------------------------------------------------------------------------------------------------------

grammar Generator {

{{{
    val tokenizer = SkipTokenizer(Lexer())
}}}

    regex ws: [ \n\r\t]+ ; // White space
    regex comment: ('/*'.*'*/')! || '//' [^\n]* ;

{{{
    init {
        tokenizer.addSkipped(ws.id)
        tokenizer.addSkipped(comment.id)
    }

    fragment hex: [0-9A-Fa-f] ;
    fragment digit: [0-9] ;

    num: regex(digit{1,6}, `toInt`)

    id: regex([A-Za-z_][A-Za-z_0-9]*) ; // regexes are always converted to strings unless a second parameter is given.
    rawString: regex(('\''.*'\'')!, `rawString`) ;
    inlined: regex(('{{{' .* '}}}')!, `mlInline` | regex(('`' .* '`')!, `slInline`) ;

    chr: // returns a codepoint
        [^\]
        | regex('\U' hex{8}, `escHex`)
        | regex('\u' hex{4}, `escHex`)
        | regex('\x' hex{2}, `escHex`)
        | regex('\\' ., `escChar`)
        ;

    setItem: chr ('-' chr >> `rangeChar` | `singleChar`)
    setItems: `emptyList` (setItem >> `append` )* ;
    charSet: '[' setItems ']' `positiveSet` | '[^' setItems ']' `negativeSet` ;
    anyChar: '.' `anyChar` ;
    regex: `regex` '(' expr (',' elementary >> `regexParser` | `regexParser.withRight(toString)`) ')' ;
    elementary: '(' expr ')' | regex | id `identifier` | string | inlined | anyChar | charSet ;
    range: num (',' (num >> `fromTo` | `atLeast`) | `exactly`) ;
    basic: elementary ( '*' `star` | '+' `plus` | '?' `option` | '!' `stop` | '{' range '}' `range` | '~' elementary >> `delimit` )? ;
    extended: basic ('>>' elementary >> `fold` | '@' elementary >> `annotate`)* ;
    concat: extended (extended >> `concat`)* ;
    union: concat ('|' concat >> `union` )* ;
    expr<`GenNode`>: union ;

    rule: fragmentRule | regexRule | parserRule ;

    fragmentRule: 'fragment' id ':' expr >> `regexRule` ;
    regexRule: 'regex' id ':' expr >> `regexRule` ;
    parserRule: id ('<' elementary '>' >> `typedRuleHeader` | `untypedRuleHeader`) ':' expr >> `parserRule` ;

    statement: regexRule | parserRule | inlined `inlined` ;
    statements: `emptyList` (statement '>>' `append`)* ;

    grammar: 'grammar' id '{' (statement* >> `grammar` ) '}' ;
}
--------------------------------------------------------------------------------------------------------------------- */

object Generator {

    private val tokenizer = SkipTokenizer(Lexer())
    private val context = Context(tokenizer)

    val ws = context.parser(RegexParser.parse("[ \\n\\r\\t]+"))
    val comment = context.parser(RegexParser.parse("('/*'.*'*/')! | '//' [^\\n]*"))

    init {
        tokenizer.addSkipped(ws.tokenId)
        tokenizer.addSkipped(comment.tokenId)
    }

    // fragment hex: [0-9A-Fa-f] ;
    val hex = RegexParser.parse("[0-9A-Fa-f]") // in the final version all regexes are expanded.

    // fragment digit: [0-9] ;
    val digit = RegexParser.parse("[0-9]")

    // num: regex(digit{1,6}, `toInt`)
    val num = context.parser(digit.range(1, 6)) { it.fold(0, { num, ch -> num * 10 + ch.toInt() - '0'.toInt()})}.ref("num")

    // id: regex([A-Za-z_][A-Za-z_0-9]*) ; // regexes are always converted to strings unless a second parameter is given.
    val identifier = context.parser(RegexParser.parse("[A-Za-z_][A-Za-z_0-9]*")) { it.toString() }.ref("identifier")

    // rawString: regex(('\''.*'\'')!, `rawString`)
    private fun unquoteRawString(seq: CharSequence): String {
        val sb = StringBuilder()

        var i = 1
        while(i < seq.length - 1) {
            if(seq[i] == '\\' && seq[i+1] == '\'') {
                sb.append("'")
                i++
            } else if(seq[i] == '\\' && seq[i+1] == '\\') {
                sb.append("\\")
                i++
            } else {
                sb.append(seq[i])
            }

            i++
        }

        return sb.toString()
    }

    val rawString = context.parser(RegexParser.parse("('\\''.*'\\'')!")) { unquoteRawString(it) }.ref("rawString")

    // Escaped Characters

    private val escContext = Context(Lexer())

    private fun hexDigit(ch: Char): Int {
        return when {
            ch <= '9' -> ch - '0'
            ch <= 'F' -> ch - 'A' + 10
            else -> ch - 'a' + 10
        }
    }

    private fun escHex(seq: CharSequence): Int {
        var ret = 0
        for(i in 2 until seq.length) {
            ret = ret * 16 + hexDigit(seq[i])
        }
        return ret
    }

    private fun escChar(ch: Char): Int {
        return when(ch) {
            'n' -> '\n'.toInt()
            'r' -> '\r'.toInt()
            't' -> '\t'.toInt()
            'b' -> '\b'.toInt()
            else -> ch.toInt()
        }
    }

    val escChr = escContext.parser(Regex.text("\\U").then(hex.count(8))) { escHex(it )}
        .or(escContext.parser(Regex.text("\\u").then(hex.count(4))) { escHex(it )})
        .or(escContext.parser(Regex.text("\\x").then(hex.count(2))) { escHex(it )})
        .or(escContext.parser(CharSet.chars('\\'.toInt()).then(CharSet.all())) { escChar(it[1])})

    private val stringContext = Context(Lexer(tokenizer.lexer().tokenIdProvider))

    val strChr = stringContext.parser(CharSet.chars('\\'.toInt(), '"'.toInt()).invert()) { it[0].toInt() }.or(escChr)

    val escString = stringContext.text("\"").then(Initializer {""}).then(
        Reducer.rep(strChr.fold {_, str: String, cp -> str + String(Character.toChars(cp))} )
        .then(stringContext.text("\""))
    )

    // CharSet

    private val charSetContext = Context(Lexer(tokenizer.lexer().tokenIdProvider))

    val setChr = charSetContext.parser(CharSet.chars('\\'.toInt(), ']'.toInt()).invert()) { it[0].toInt() }.or(escChr)

    // chr ('-' chr >> `rangeChar` | `singleChar`)
    val setItem = setChr.then(
        charSetContext.text("-").then(setChr).fold { _, lo: Int, hi -> CharSet.interval(lo, hi) }
            .or(Mapping{ _, ch -> CharSet.chars(ch)})
    )

    val setItems = Initializer{CharSet.empty()}.then(Reducer.rep(setItem.fold { _, set: CharSet, item -> set.union(item) }))

    // charSet: '[' setItems ']' `positiveSet` | '[^' setItems ']' `negativeSet` ;
    val charSet = context.text("[").then(setItems).then(charSetContext.text("]"))
        .or(context.text("[^").then(setItems).then(charSetContext.text("]")).then(Mapping{ _, set -> set.invert()}))

    val anyChar = context.text(".").then(Initializer<CharSet>{CharSet.all()})

    // Now for the rest
    //     inlined: regex(('{{{' .* '}}}')!, `mlInline` | regex(('`' .* '`')!, `slInline`) ;
    val code = context.parser(RegexParser.parse("('{{{'.*'}}}')!")) {it.subSequence(3, it.length - 3).toString()}
        .or(context.parser(RegexParser.parse("('`'.*'`')!")) {it.subSequence(1, it.length - 1).toString()}).ref("code")

    val expr = Ref<GenNode>("expr")
    val elementary = Ref<GenNode>("elementary")

    class RegexParserNode(info: SourceInfo, val regex: GenNode, val fn: GenNode): GenNode(info) {
        override fun toCode(): String {
            return "context.parser(${regex.toRegex()}, ${fn.toCode()})"
        }
    }

    // regex: 'regex' '(' expr ',' elementary >> `regexParser` ')' ;
    val regexParser = context.text("regex").then(context.text("(")).then(
        expr.then(context.text(",").then(elementary).fold { stream, regex: GenNode, fn: GenNode -> RegexParserNode(stream.createSourceInfo(), regex, fn) as GenNode })
    ).then(context.text(")"))

    class IdNode(info: SourceInfo, val id: String): GenNode(info) {
        override fun toCode(): String {
            return id
        }

        override fun toRegex(): String {
            return id
        }
    }

    class StringNode(info: SourceInfo, val string: String): GenNode(info) {
        private fun mapChar(ch: Char): String {
            return when {
                ch == '\n' -> "\\n"
                ch == '\r' -> "\\r"
                ch == '\t' -> "\\t"
                ch == '\b' -> "\\b"
                ch == '\\' -> "\\\\"
                ch == '"' -> "\\\""
                ch < ' ' || ch >= 0xff.toChar() -> String.format("\\u%04x", ch.toInt())
                else -> ch.toString()
            }
        }

        private fun javaString(): String {
            // FIXME move to parsing
            val sb = StringBuilder()
            string.forEach {sb.append(mapChar(it))}
            return sb.toString()
        }

        override fun toCode(): String {
            return "context.text(\"${javaString()}\")"
        }

        override fun toRegex(): String {
            return "Regex.text(\"${javaString()}\")"
        }
    }

    class CodeNode(info: SourceInfo, val code: String): GenNode(info) {
        override fun toCode(): String {
            return code
        }

        override fun toRegex(): String {
            return code
        }
    }

    class CharSetNode(info: SourceInfo, val set: CharSet): GenNode(info) {
        override fun toRegex(): String {
            val intervals = set.map { "${it.start}, ${it.end-1}" }.joinToString(", ")
            return "CharSet.interval($intervals)"
        }
    }

    // elementary: '(' expr ')' | regex | id `identifier` | string | inlined | anyChar | charSet ;
    init {
        elementary.set(
            context.text("(").then(expr).then(context.text(")"))
                .or(regexParser)
                .or(identifier.then(Mapping{stream, id -> IdNode(stream.createSourceInfo(), id)}))
                .or(rawString.or(escString).then(Mapping{ stream, str -> StringNode(stream.createSourceInfo(), str)}))
                .or(code.then(Mapping{ stream, code -> CodeNode(stream.createSourceInfo(), code)}))
                .or(anyChar.or(charSet).then(Mapping{ stream, set -> CharSetNode(stream.createSourceInfo(), set)}))
        )
    }

    class Range(val lo: Int, val hi: Int)

    //    range: num (',' (num >> `fromTo` | `atLeast`) | `exactly`) ;
    val range = num.then(
            context.text(",").then(num.fold{_, lo: Int, hi -> Range(lo, hi)}.or(Mapping{_, mini -> Range(mini, -1)}))
                .or(Mapping{_, count: Int -> Range(count, count)})
        )

    // basic: elementary ( '*' `star` | '+' `plus` | '?' `option` | '!' `stop` | '{' range '}' `range` )* ;
    class Star(info: SourceInfo, val expr: GenNode): GenNode(info) {
        override fun toCode(): String {
            return "Reducer.rep(${expr.toCode()})"
        }

        override fun toRegex(): String {
            return "${expr.toRegex()}.rep()"
        }
    }

    class Plus(info: SourceInfo, val expr: GenNode): GenNode(info){
        override fun toCode(): String {
            return "Reducer.plus(${expr.toCode()})"
        }

        override fun toRegex(): String {
            return "${expr.toRegex()}.plus()"
        }
    }


    class Opt(info: SourceInfo, val expr: GenNode): GenNode(info){
        override fun toCode(): String {
            return "Reducer.opt(${expr.toCode()})"
        }

        override fun toRegex(): String {
            return "${expr.toRegex()}.opt()"
        }
    }

    class Stop(info: SourceInfo, val expr: GenNode): GenNode(info){
        override fun toRegex(): String {
            return "${expr.toRegex()}.nonGreedy()"
        }
    }

    class RangeNode(info: SourceInfo, val expr: GenNode, val range: Range): GenNode(info){
        override fun toRegex(): String {
            if(range.lo == range.hi) return "${expr.toRegex()}.count(${range.lo})"
            if(range.hi < 0) return "${expr.toRegex()}.min(${range.lo})"
            return "${expr.toRegex()}.range(${range.lo}, ${range.hi})"
        }
    }

    val basic = elementary.then(Reducer.rep(
        context.text("*").then(Mapping{stream, expr: GenNode -> Star(stream.createSourceInfo(), expr) as GenNode })
            .or(context.text("+").then(Mapping{stream, expr: GenNode -> Plus(stream.createSourceInfo(), expr) as GenNode } ))
            .or(context.text("?").then(Mapping{stream, expr: GenNode -> Opt(stream.createSourceInfo(), expr) as GenNode } ))
            .or(context.text("!").then(Mapping{stream, expr: GenNode -> Stop(stream.createSourceInfo(), expr) as GenNode } ))
            .or(context.text("{").then(range.fold { stream, expr: GenNode, range -> RangeNode(stream.createSourceInfo(), expr, range) as GenNode }).then(context.text("}")))
    ))

    class FoldNode(info: SourceInfo, val expr: GenNode, val fold: GenNode): GenNode(info) {
        override fun toCode(): String {
            return "${expr.toCode()}.fold(${fold.toCode()})"
        }
    }

    class AnnotationNode(info: SourceInfo, val expr: GenNode, val annotation: GenNode): GenNode(info) {
        override fun toCode(): String {
            return "${expr.toCode()}.annotate(${annotation.toCode()})"
        }
    }

    //        extended: basic ('>>' elementary >> `fold` | '@' elementary >> `annotate`)* ;

    val extended = basic.then(Reducer.rep(
        context.text(">>").then(elementary).fold{stream, expr: GenNode, fold -> FoldNode(stream.createSourceInfo(), expr, fold) as GenNode}
        .or(context.text("@").then(elementary).fold{stream, expr: GenNode, annotation -> AnnotationNode(stream.createSourceInfo(), expr, annotation) as GenNode })
    ))

    // concat: extended (extended >> `concat`)* ;
    class ConcatNode(info: SourceInfo, val left: GenNode, val right: GenNode): GenNode(info) {
        override fun toCode(): String {
            return "${left.toCode()}.then(${right.toCode()})"
        }

        override fun toRegex(): String {
            return "${left.toRegex()}.then(${right.toRegex()})"
        }
    }

    val concat = extended.then(Reducer.rep(extended.fold{stream, left: GenNode, right -> ConcatNode(stream.createSourceInfo(), left, right) as GenNode}))

    // union: concat ('|' concat >> `union` )* ;
    class UnionNode(info: SourceInfo, val left: GenNode, val right: GenNode): GenNode(info) {
        override fun toCode(): String {
            return "${left.toCode()}.or(${right.toCode()})"
        }

        override fun toRegex(): String {
            return "${left.toRegex()}.or(${right.toRegex()})"
        }
    }

    val union = concat.then(Reducer.rep(context.text("|").then(concat.fold{stream, left: GenNode, right -> UnionNode(stream.createSourceInfo(), left, right) as GenNode})))

    // expr<`GenNode`>: union ;
    init {
        expr.set(union)
    }

    class FragmentRuleNode(info: SourceInfo, val lhs: String, val rhs: GenNode): GenNode(info) {
        override fun toCode(): String {
            return "    val $lhs: Regex = ${rhs.toRegex()}"
        }
    }


    // fragmentRule: 'fragment' id ':' expr >> `regexRule` ;
    val fragmentRule = context.text("fragment").then(
        identifier.then(context.text(":").then(expr)
            .fold { stream, id: String, rhs -> FragmentRuleNode(stream.createSourceInfo(), id, rhs) as GenNode})).ref("fragmentRule")

    class RegexRuleNode(info: SourceInfo, val lhs: String, val rhs: GenNode): GenNode(info) {
        override fun toCode(): String {
            return "    val $lhs = context.parser(${rhs.toRegex()})"
        }
    }

    // regexRule: 'regex' id ':' expr >> `regexRule` ;
    val regexRule = context.text("regex").then(
        identifier.then(context.text(":").then(expr)
            .fold { stream, id: String, rhs -> RegexRuleNode(stream.createSourceInfo(), id, rhs) as GenNode})).ref("regexRule")

    class TypedRuleHeader(info: SourceInfo, val name: String, val type: GenNode?): GenNode(info)

    // typedHeader: id ('<' elementary '>' >> `typedRuleHeader` | `untypedRuleHeader`)
    val typedHeader = identifier.then(
        context.text("<").then(elementary).then(context.text(">")).fold { stream, id: String, type: GenNode -> TypedRuleHeader(stream.createSourceInfo(), id, type)}
            .or(Mapping{stream, id -> TypedRuleHeader(stream.createSourceInfo(), id, null)})
    ).ref("typedHeader")

    class ParserRuleNode(info: SourceInfo, val lhs: TypedRuleHeader, val rhs: GenNode): GenNode(info) {
        override fun toCode(): String {
            if(lhs.type == null) {
                return "    val ${lhs.name} = ${rhs.toCode()}"
            }

            return "    init {\n        ${lhs.name}.set(${rhs.toCode()})\n    }"
        }
    }

    // parserRule: typedHeader ':' expr >> `parserRule` ;
    val parserRule =
        typedHeader.then(context.text(":").then(expr)
            .fold { stream, header: TypedRuleHeader, rhs -> ParserRuleNode(stream.createSourceInfo(), header, rhs) as GenNode})

    // rule: fragmentRule | regexRule | parserRule ;
    val rule = fragmentRule.or(regexRule).or(parserRule).ref("rule")

    // statement: regexRule | parserRule | inlined `inlined` ;
    val statement = rule.then(context.text(";")).or(code.then(Mapping{ stream, code -> CodeNode(stream.createSourceInfo(), code)})).ref("statement")

    // statements: `emptyList` (statement '>>' `append`)* ;
    val statements = EmptyList<GenNode>().then(Reducer.rep(statement.fold(Append(0)))).ref("statements")

    // grammar: 'grammar' id '{' (statement* >> `grammar` ) '}' ;
    class Grammar(info: SourceInfo, val name: String, val content: List<GenNode>): GenNode(info) {
        override fun toCode(): String {
            val writer = StringBuilder()
            writer.append("object $name {\n")

            val predefinedRefs = content.filterIsInstance<ParserRuleNode>().map{it.lhs}.filter{it.type != null}

            // typed parser rules are predefined as refs.
            predefinedRefs.forEach {
                writer.append("    val ${it.name} = Ref<${it.type!!.toCode()}>(\"${it.name}\")\n")
            }

            content.forEach {
                writer.append("    // position ${it.sourceInfo()}\n")
                writer.append(it.toCode())
                writer.append("\n\n")
            }

            writer.append("}")

            return writer.toString()
        }
    }

    val grammar = context.text("grammar")
        .then(identifier)
        .then(context.text("{"))
        .then(statements.fold{ stream, name: String, stmts -> Grammar(stream.createSourceInfo(), name, stmts) })
        .then(context.text("}")).ref("grammar")

    class Program(info: SourceInfo, val header: String, val grammar: Grammar): GenNode(info) {
        override fun toCode(): String {
            return "${header}${grammar.toCode()}"
        }
    }

    val program = code.or(Initializer{""}).
        then(grammar.fold{ stream, header: String, body: Grammar -> Program(stream.createSourceInfo(), header, body)})
}