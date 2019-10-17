package at.searles.parsing.tools.generator

import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.lexer.Tokenizer
import at.searles.lexer.utils.Counter
import at.searles.lexer.utils.Interval
import at.searles.parsing.*
import at.searles.parsing.tokens.TokenParser
import at.searles.parsing.utils.Utils
import at.searles.parsing.utils.ast.AstNode
import at.searles.parsing.utils.ast.SourceInfo
import at.searles.parsing.utils.common.ToString
import at.searles.regex.CharSet
import at.searles.regex.RegexParser
import at.searles.regex.Regex

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
    expr<`AstNode`>: union ;

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
    class Context(val tokenizer: Tokenizer) {
        fun parser(regex: at.searles.regex.Regex): RexParser<CharSequence> {
            return RexParser(regex, tokenizer, true, Mapping { _, seq -> seq })
        }

        fun <T> parser(regex: at.searles.regex.Regex, fn: (CharSequence) -> T): RexParser<T> {
            return RexParser(regex, tokenizer, true, Mapping { _, seq -> fn(seq) })
        }

        fun text(text: String): Recognizer {
            return Recognizer.fromString(text, tokenizer, false)
        }
    }

    class RexParser<T>(val regex: at.searles.regex.Regex, val tokenizer: Tokenizer, exclusive: Boolean, mapping: Mapping<CharSequence, T>): Parser<T> {
        val id = tokenizer.add(regex)
        private val parser = Parser.fromToken(id, tokenizer, exclusive, mapping)

        override fun parse(stream: ParserStream): T? {
            return parser.parse(stream)
        }

        override fun recognize(stream: ParserStream): Boolean {
            return parser.recognize(stream)
        }
    }

    private val tokenIdCounter = Counter()
    private val tokenizer = SkipTokenizer(Lexer(tokenIdCounter))
    private val context = Context(tokenizer)

    val ws = context.parser(RegexParser.parse("[ \\n\\r\\t]+"))
    val comment = context.parser(RegexParser.parse("('/*'.*'*/')! | '//' [^\\n]*"))

    init {
        tokenizer.addSkipped(ws.id)
        tokenizer.addSkipped(comment.id)
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
        if(ch <= '9') return ch - '0'
        if(ch <= 'F') return ch - 'F'
        return ch - 'f'
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

    private val stringContext = Context(Lexer(tokenIdCounter))

    val strChr = stringContext.parser(CharSet.chars('\\'.toInt(), '"'.toInt()).invert()) { it[0].toInt() }.or(escChr)

    val escString = stringContext.text("\"").then(Initializer {""}).then(
        Reducer.rep(strChr.fold {_, str: String, cp -> str + String(Character.toChars(cp))} )
        .then(stringContext.text("\""))
    )

    // CharSet

    private val charSetContext = Context(Lexer(tokenIdCounter))

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
    val code = context.parser(RegexParser.parse("('{{{' .* '}}}')!")) {it.subSequence(3, it.length - 3).toString()}
        .or(context.parser(RegexParser.parse("('`' .* '`')!")) {it.subSequence(1, it.length - 1).toString()})

    val expr = Ref<AstNode>("expr")
    val elementary = Ref<AstNode>("elementary")

    class RegexParserNode(info: SourceInfo, val regex: AstNode, val fn: AstNode): AstNode(info)

    // regex: 'regex' '(' expr ',' elementary >> `regexParser` ')' ;
    val regexParser = context.text("regex").then(context.text("(")).then(
        expr.then(context.text(",").then(elementary).fold { stream, regex: AstNode, fn: AstNode -> RegexParserNode(stream.createSourceInfo(), regex, fn) as AstNode })
    )

    class IdNode(info: SourceInfo, val id: String): AstNode(info)
    class StringNode(info: SourceInfo, val string: String): AstNode(info)
    class CodeNode(info: SourceInfo, val code: String): AstNode(info)
    class CharSetNode(info: SourceInfo, val set: CharSet): AstNode(info)

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
    class Star(info: SourceInfo, val expr: AstNode): AstNode(info)
    class Plus(info: SourceInfo, val expr: AstNode): AstNode(info)
    class Opt(info: SourceInfo, val expr: AstNode): AstNode(info)
    class Stop(info: SourceInfo, val expr: AstNode): AstNode(info)
    class RangeNode(info: SourceInfo, val expr: AstNode, val range: Range): AstNode(info)

    val basic = elementary.then(Reducer.rep(
        context.text("*").then(Mapping{stream, expr: AstNode -> Star(stream.createSourceInfo(), expr) as AstNode })
            .or(context.text("+").then(Mapping{stream, expr: AstNode -> Plus(stream.createSourceInfo(), expr) as AstNode } ))
            .or(context.text("?").then(Mapping{stream, expr: AstNode -> Opt(stream.createSourceInfo(), expr) as AstNode } ))
            .or(context.text("!").then(Mapping{stream, expr: AstNode -> Stop(stream.createSourceInfo(), expr) as AstNode } ))
            .or(range.fold { stream, expr: AstNode, range -> RangeNode(stream.createSourceInfo(), expr, range) as AstNode })
    ))

    class FoldNode(info: SourceInfo, val expr: AstNode, val fold: AstNode): AstNode(info)
    class AnnotationNode(info: SourceInfo, val expr: AstNode, val annotation: AstNode): AstNode(info)

    //        extended: basic ('>>' elementary >> `fold` | '@' elementary >> `annotate`)* ;

    val extended = basic.then(Reducer.rep(
        context.text(">>").then(elementary).fold{stream, expr: AstNode, fold -> FoldNode(stream.createSourceInfo(), expr, fold) as AstNode}
        .or(context.text("@").then(elementary).fold{stream, expr: AstNode, annotation -> AnnotationNode(stream.createSourceInfo(), expr, annotation) as AstNode })
    ))

    // concat: extended (extended >> `concat`)* ;
    class ConcatNode(info: SourceInfo, val left: AstNode, val right: AstNode): AstNode(info)
    val concat = extended.then(Reducer.rep(extended.fold{stream, left: AstNode, right -> ConcatNode(stream.createSourceInfo(), left, right) as AstNode}))

    // union: concat ('|' concat >> `union` )* ;
    class UnionNode(info: SourceInfo, val left: AstNode, val right: AstNode): AstNode(info)
    val union = concat.then(Reducer.rep(context.text("|").then(concat.fold{stream, left: AstNode, right -> ConcatNode(stream.createSourceInfo(), left, right) as AstNode})))

    // expr<`AstNode`>: union ;
    init {
        expr.set(union)
    }

    /*fragmentRule: 'fragment' id ':' expr >> `regexRule` ;
    regexRule: 'regex' id ':' expr >> `regexRule` ;
    parserRule: id ('<' elementary '>' >> `typedRuleHeader` | `untypedRuleHeader`) ':' expr >> `parserRule` ;

    rule: fragmentRule | regexRule | parserRule ;


    statement: regexRule | parserRule | inlined `inlined` ;
    statements: `emptyList` (statement '>>' `append`)* ;

    grammar: 'grammar' id '{' (statement* >> `grammar` ) '}' ;*/

}