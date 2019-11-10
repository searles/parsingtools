package at.searles.parsingtools.generator

import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.*
import at.searles.parsingtools.common.CreateValue
import at.searles.parsingtools.list.AddToList
import at.searles.regex.CharSet
import at.searles.regexparser.StringToRegex
import at.searles.regex.Regex
import at.searles.parsingtools.list.CreateEmptyList

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
    regex: `regex` '(' expr (',' elementary >> `StringToRegex` | `StringToRegex.withRight(toString)`) ')' ;
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
    private val context =
        Context(tokenizer)

    val ws = context.parser(StringToRegex.parse("[ \\n\\r\\t]+"))
    val comment = context.parser(StringToRegex.parse("('/*'.*'*/')! | '//' [^\\n]*"))

    init {
        tokenizer.addSkipped(ws.tokenId)
        tokenizer.addSkipped(comment.tokenId)
    }

    // fragment hex: [0-9A-Fa-f] ;
    val hex = StringToRegex.parse("[0-9A-Fa-f]") // in the final version all regexes are expanded.

    // fragment digit: [0-9] ;
    val digit = StringToRegex.parse("[0-9]")

    // num: regex(digit{1,6}, `toInt`)
    val num = context.parser(digit.range(1, 6)) { it.fold(0, { num, ch -> num * 10 + ch.toInt() - '0'.toInt()})}.ref("num")

    // id: regex([A-Za-z_][A-Za-z_0-9]*) ; // regexes are always converted to strings unless a second parameter is given.
    val identifier = context.parser(StringToRegex.parse("[A-Za-z_][A-Za-z_0-9]*")) { it.toString() }.ref("identifier")

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

    val rawString = context.parser(StringToRegex.parse("('\\''.*'\\'')!")) {
        unquoteRawString(
            it
        )
    }.ref("rawString")

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

    val escChr = escContext.parser(Regex.text("\\U").then(hex.count(8))) {
        escHex(
            it
        )
    }
        .or(escContext.parser(Regex.text("\\u").then(hex.count(4))) {
            escHex(
                it
            )
        })
        .or(escContext.parser(Regex.text("\\x").then(hex.count(2))) {
            escHex(
                it
            )
        })
        .or(escContext.parser(CharSet.chars('\\'.toInt()).then(CharSet.all())) {
            escChar(
                it[1]
            )
        })

    private val stringContext =
        Context(Lexer(tokenizer.lexer().tokenIdProvider))

    val strChr = stringContext.parser(CharSet.chars('\\'.toInt(), '"'.toInt()).invert()) { it[0].toInt() }.or(
        escChr
    )

    val escString = stringContext.text("\"").then(Initializer {""}).then(
        Reducer.rep(strChr.fold { _, str: String, cp -> str + String(Character.toChars(cp))} )
        .then(stringContext.text("\""))
    )

    // CharSet

    private val charSetContext =
        Context(Lexer(tokenizer.lexer().tokenIdProvider))

    val setChr = charSetContext.parser(CharSet.chars('\\'.toInt(), ']'.toInt()).invert()) { it[0].toInt() }.or(
        escChr
    )

    // chr ('-' chr >> `rangeChar` | `singleChar`)
    val setItem = setChr.then(
        charSetContext.text("-").then(setChr).fold { _, lo: Int, hi -> CharSet.interval(lo, hi) }
            .or(Mapping{ _, ch -> CharSet.chars(ch)})
    )

    val setItems = Initializer{CharSet.empty()}.then(Reducer.rep(setItem.fold { _, set: CharSet, item -> set.union(item) }))

    // charSet: '[' setItems ']' `positiveSet` | '[^' setItems ']' `negativeSet` ;
    val charSet = context.text("[").then(setItems).then(
        charSetContext.text("]"))
        .or(
            context.text("[^").then(setItems).then(
                charSetContext.text("]")).then(Mapping{ _, set -> set.invert()}))

    val anyChar = context.text(".").then(Initializer<CharSet>{CharSet.all()})

    // Now for the rest
    //     inlined: regex(('{{{' .* '}}}')!, `mlInline` | regex(('`' .* '`')!, `slInline`) ;
    val code = context.parser(StringToRegex.parse("('{{{'.*'}}}')!")) {it.subSequence(3, it.length - 3).toString()}
        .or(context.parser(StringToRegex.parse("('`'.*'`')!")) {it.subSequence(1, it.length - 1).toString()}).ref("code")

    val expr = Ref<VisitorNode>("expr")
    val elementary = Ref<VisitorNode>("elementary")

    class RegexParserNode(stream: ParserStream, val regex: VisitorNode, val fn: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    // regex: 'regex' '(' expr ',' elementary >> `RegexParser` ')' ;
    val RegexParser = context.text("regex").then(context.text("(")).then(
        expr.then(
            context.text(",").then(
                elementary
            ).fold { stream, regex: VisitorNode, fn: VisitorNode -> RegexParserNode(
                stream,
                regex,
                fn
            ) as VisitorNode
            })
    ).then(context.text(")"))

    class IdNode(stream: ParserStream, val id: String): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    class StringNode(stream: ParserStream, val string: String): VisitorNode(stream.createTrace()) {
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

        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    class CodeNode(stream: ParserStream, val code: String): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    class CharSetNode(stream: ParserStream, val set: CharSet): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    // elementary: '(' expr ')' | regex | id `identifier` | string | inlined | anyChar | charSet ;
    init {
        elementary.set(
            context.text("(").then(expr).then(
                context.text(")"))
                .or(RegexParser)
                .or(identifier.then(Mapping{ stream, id ->
                    IdNode(
                        stream,
                        id
                    )
                }))
                .or(rawString.or(escString).then(Mapping{ stream, str ->
                    StringNode(
                        stream,
                        str
                    )
                }))
                .or(code.then(Mapping{ stream, code ->
                    CodeNode(
                        stream,
                        code
                    )
                }))
                .or(anyChar.or(charSet).then(Mapping{ stream, set ->
                    CharSetNode(
                        stream,
                        set
                    )
                }))
        )
    }

    class Range(val lo: Int, val hi: Int)

    //    range: num (',' (num >> `fromTo` | `atLeast`) | `exactly`) ;
    val range = num.then(
            context.text(",").then(num.fold{ _, lo: Int, hi ->
                Range(
                    lo,
                    hi
                )
            }.or(Mapping{ _, mini -> Range(mini, -1) }))
                .or(Mapping{_, count: Int -> Range(count, count) })
        )

    // basic: elementary ( '*' `star` | '+' `plus` | '?' `option` | '!' `stop` | '{' range '}' `range` )* ;
    class Star(stream: ParserStream, val expr: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    class Plus(stream: ParserStream, val expr: VisitorNode): VisitorNode(stream.createTrace()){
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }


    class Opt(stream: ParserStream, val expr: VisitorNode): VisitorNode(stream.createTrace()){
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    class Stop(stream: ParserStream, val expr: VisitorNode): VisitorNode(stream.createTrace()){
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    class RangeNode(stream: ParserStream, val expr: VisitorNode, val range: Range): VisitorNode(stream.createTrace()){
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    val basic = elementary.then(Reducer.rep(
        context.text("*").then(Mapping{ stream, expr: VisitorNode -> Star(
            stream,
            expr
        ) as VisitorNode
        })
            .or(context.text("+").then(Mapping{ stream, expr: VisitorNode -> Plus(
                stream,
                expr
            ) as VisitorNode
            } ))
            .or(context.text("?").then(Mapping{ stream, expr: VisitorNode -> Opt(
                stream,
                expr
            ) as VisitorNode
            } ))
            .or(context.text("!").then(Mapping{ stream, expr: VisitorNode -> Stop(
                stream,
                expr
            ) as VisitorNode
            } ))
            .or(context.text("{").then(range.fold { stream, expr: VisitorNode, range -> RangeNode(
                stream,
                expr,
                range
            ) as VisitorNode
            }).then(context.text("}")))
    ))

    class FoldNode(stream: ParserStream, val expr: VisitorNode, val fold: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    class AnnotationNode(stream: ParserStream, val expr: VisitorNode, val annotation: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    //        extended: basic ('>>' elementary >> `fold` | '@' elementary >> `annotate`)* ;

    val extended = basic.then(Reducer.rep(
        context.text(">>").then(elementary).fold{ stream, expr: VisitorNode, fold -> FoldNode(
            stream,
            expr,
            fold
        ) as VisitorNode
        }
        .or(context.text("@").then(elementary).fold{ stream, expr: VisitorNode, annotation -> AnnotationNode(
            stream,
            expr,
            annotation
        ) as VisitorNode
        })
    ))

    // concat: extended (extended >> `concat`)* ;
    class ConcatNode(stream: ParserStream, val left: VisitorNode, val right: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    val concat = extended.then(Reducer.rep(extended.fold{ stream, left: VisitorNode, right -> ConcatNode(
        stream,
        left,
        right
    ) as VisitorNode
    }))

    // union: concat ('|' concat >> `union` )* ;
    class UnionNode(stream: ParserStream, val left: VisitorNode, val right: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    val union = concat.then(Reducer.rep(
        context.text("|").then(
            concat.fold{ stream, left: VisitorNode, right -> UnionNode(
                stream,
                left,
                right
            ) as VisitorNode
            })))

    // expr<`GenNode`>: union ;
    init {
        expr.set(union)
    }

    class FragmentRuleNode(stream: ParserStream, val lhs: String, val rhs: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }


    // fragmentRule: 'fragment' id ':' expr >> `regexRule` ;
    val fragmentRule = context.text("fragment").then(
        identifier.then(
            context.text(":").then(expr)
            .fold { stream, id: String, rhs -> FragmentRuleNode(
                stream,
                id,
                rhs
            ) as VisitorNode
            })).ref("fragmentRule")

    class RegexRuleNode(stream: ParserStream, val lhs: String, val rhs: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    // regexRule: 'regex' id ':' expr >> `regexRule` ;
    val regexRule = context.text("regex").then(
        identifier.then(
            context.text(":").then(expr)
            .fold { stream, id: String, rhs -> RegexRuleNode(
                stream,
                id,
                rhs
            ) as VisitorNode
            })).ref("regexRule")

    class TypedRuleHeader(val trace: Trace, val name: String, val type: VisitorNode?)

    // typedHeader: id ('<' elementary '>' >> `typedRuleHeader` | `untypedRuleHeader`)
    val typedHeader = identifier.then(
        context.text("<").then(elementary).then(
            context.text(">")).fold { stream, id: String, type: VisitorNode ->
            TypedRuleHeader(
                stream.createTrace(),
                id,
                type
            )
        }
            .or(Mapping{stream, id -> TypedRuleHeader(stream.createTrace(), id, null) })
    ).ref("typedHeader")

    class ParserRuleNode(stream: ParserStream, val lhs: TypedRuleHeader, val rhs: VisitorNode): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    // parserRule: typedHeader ':' expr >> `parserRule` ;
    val parserRule =
        typedHeader.then(
            context.text(":").then(expr)
            .fold { stream, header: TypedRuleHeader, rhs -> ParserRuleNode(
                stream,
                header,
                rhs
            ) as VisitorNode
            })

    // rule: fragmentRule | regexRule | parserRule ;
    val rule = fragmentRule.or(regexRule).or(
        parserRule
    ).ref("rule")

    // statement: regexRule | parserRule | inlined `inlined` ;
    val statement = rule.then(context.text(";")).or(
        code.then(Mapping{ stream, code ->
            CodeNode(
                stream,
                code
            )
        })).ref("statement")

    // statements: `emptyList` (statement '>>' `append`)* ;
    val statements = CreateEmptyList<VisitorNode>().then(Reducer.rep(
        statement.fold(
            AddToList(0)
        ))).ref("statements")

    // grammar: 'grammar' id '{' (statement* >> `grammar` ) '}' ;
    class Grammar(stream: ParserStream, val name: String, val content: List<VisitorNode>): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit((this))
        }
    }

    val grammar = context.text("grammar")
        .then(identifier)
        .then(context.text("{"))
        .then(statements.fold{ stream, name: String, stmts ->
            Grammar(
                stream,
                name,
                stmts
            )
        })
        .then(context.text("}")).ref("grammar")

    class Program(stream: ParserStream, val header: String, val grammar: Grammar): VisitorNode(stream.createTrace()) {
        override fun <A> accept(visitor: Visitor<A>): A {
            return visitor.visit(this)
        }
    }

    val program = code.or(CreateValue("")).
        then(grammar.fold{ stream, header: String, body: Grammar ->
            Program(
                stream,
                header,
                body
            )
        })
}