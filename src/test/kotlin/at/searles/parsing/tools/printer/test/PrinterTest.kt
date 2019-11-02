package at.searles.parsing.tools.printer.test

import at.searles.buf.Frame
import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.lexer.TokenStream
import at.searles.parsing.*
import at.searles.parsing.printing.*
import at.searles.parsing.tools.common.SyntaxInfo
import at.searles.regexparser.StringToRegex;
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class PrinterTest {

    @Before
    fun setUp() {
        initParser()
        initCstPrinter()
    }

    @Test
    fun constantTest() {
        initParser()
        withInput("a")
        actParse()
        actPrint()

        Assert.assertEquals("a", output)
    }

    @Test
    fun constantFormatTest() {
        initParser()
        withInput("a")
        actFormat()

        Assert.assertEquals("a", output)
    }

    @Test
    fun appTest() {
        initParser()
        withInput("a b")
        actParse()
        actPrint()

        Assert.assertEquals("a b", output)
    }

    @Test
    fun appFormatTest() {
        initParser()
        withInput("  a   b  ")
        actFormat()

        Assert.assertEquals("a b", output)
    }

    @Test
    fun longAppTest() {
        initParser()
        withInput("a b c d")
        actParse()
        actPrint()

        Assert.assertEquals("a b c d", output)
    }

    @Test
    fun appInAppTest() {
        initParser()
        withInput("a ( b c )")
        actParse()
        actPrint()

        Assert.assertEquals("a (\n b c\n)", output)
    }

    @Test
    fun appInAppFormatTest() {
        initParser()
        withInput("a ( b c )")
        actFormat()

        Assert.assertEquals("a (\n b c\n)", output)
    }

    @Test
    fun manyEmbeddedAppsTest() {
        initParser()
        withInput("a ( b ( c d (e (f g h(i j) k (l m n) ) ) o p (q r (s t))))")
        actParse()
        actPrint()

        Assert.assertEquals("a (\n" +
                " b (\n" +
                "  c d (\n" +
                "   e (\n" +
                "    f g h (\n" +
                "     i j\n" +
                "    ) k (\n" +
                "     l m n\n" +
                "    )\n" +
                "   )\n" +
                "  ) o p (\n" +
                "   q r (\n" +
                "    s t\n" +
                "   )\n" +
                "  )\n" +
                " )\n" +
                ")", output)
    }

    @Test
    fun manyEmbeddedAppsFormatTest() {
        initParser()
        withInput("a ( b ( c d (e (f g h(i j) k (l m n) ) ) o p (q r (s t))))")
        actFormat()

        Assert.assertEquals("a (\n" +
                " b (\n" +
                "  c d (\n" +
                "   e (\n" +
                "    f g h (\n" +
                "     i j\n" +
                "    ) k (\n" +
                "     l m n\n" +
                "    )\n" +
                "   )\n" +
                "  ) o p (\n" +
                "   q r (\n" +
                "    s t\n" +
                "   )\n" +
                "  )\n" +
                " )\n" +
                ")", output)
    }

    private fun withInput(input: String) {
        this.stream = ParserStream.fromString(input)
    }

    private fun actFormat() {
        val stack: Stack<ArrayList<ConcreteSyntaxTree>> = Stack()

        stack.push(ArrayList())

        this.stream.setListener(object: ParserStream.Listener {
            override fun <C : Any?> annotationBegin(src: ParserStream, annotation: C) {
                stack.push(ArrayList())
            }

            override fun <C : Any?> annotationEnd(src: ParserStream, annotation: C, success: Boolean) {
                if(!success) {
                    // we created the list for nothing...
                    stack.pop()
                    return
                }

                // otherwise, add it.
                val list = stack.pop()
                val cstNode = ListConcreteSyntaxTree(list)
                stack.peek().add(AnnotatedConcreteSyntaxTree(annotation, cstNode))
            }
        })

        this.stream.tokStream().setListener(object: TokenStream.Listener {
            override fun tokenConsumed(src: TokenStream, tokId: Int, frame: Frame) {
                // skip all white spaces
                if(tokId == whiteSpaceTokId) {
                    return
                }

                // add all other tokens to current top in stack.
                // this also includes other hidden tokens like comments
                // that we normally want to keep when formatting the source 
                // code.
                stack.peek().add(LeafConcreteSyntaxTree(frame.toString()))
            }
        })

        if(!parser.recognize(stream)) {
            output = null
            return
        }

        val cst = ListConcreteSyntaxTree(stack.pop())

        assert(stack.isEmpty())

        cst.printTo(cstPrinter)
        output = outStream.toString()
    }

    private fun actParse() {
        ast = parser.parse(stream)
    }

    private fun actPrint() {
        val cst = parser.print(ast)
        cst?.printTo(cstPrinter)
        output = outStream.toString()
    }

    private var whiteSpaceTokId: Int = Integer.MIN_VALUE // invalid default value.
    private lateinit var outStream: StringOutStream
    private lateinit var stream: ParserStream
    private lateinit var parser: Parser<SyntaxInfo>
    private var ast: SyntaxInfo? = null
    private var output: String? = null

    private lateinit var cstPrinter: CstPrinter

    private fun initParser() {
        val lexer = Lexer()
        val tokenizer = SkipTokenizer(lexer)

        whiteSpaceTokId = lexer.add(StringToRegex.parse("[ \n\r\t]+"))
        tokenizer.addSkipped(whiteSpaceTokId)

        val openPar = Recognizer.fromString("(", tokenizer, false)
        val closePar = Recognizer.fromString(")", tokenizer, false)

        val idMapping = object : Mapping<CharSequence, SyntaxInfo> {
            override fun parse(stream: ParserStream, left: CharSequence): SyntaxInfo =
                IdNode(stream, left.toString())

            override fun left(result: SyntaxInfo): CharSequence? =
                    if (result is IdNode) result.value else null
        }

        val numMapping = object : Mapping<CharSequence, SyntaxInfo> {
            override fun parse(stream: ParserStream, left: CharSequence): SyntaxInfo =
                NumNode(
                    stream,
                    Integer.parseInt(left.toString())
                )

            override fun left(result: SyntaxInfo): CharSequence? =
                    if (result is NumNode) result.value.toString() else null
        }

        val id = Parser.fromToken(lexer.add(StringToRegex.parse("[a-z]+")), tokenizer, false, idMapping).ref("id")
        val num = Parser.fromToken(lexer.add(StringToRegex.parse("[0-9]+")), tokenizer, false, numMapping).ref("num")

        val expr = Ref<SyntaxInfo>("expr")

        // term = id | num | '(' expr ')'
        val term = id.or(num).or(openPar.then(expr.annotate(Markers.Block)).then(closePar))

        // app = term+
        val appFold = object : Fold<SyntaxInfo, SyntaxInfo, SyntaxInfo> {
            override fun apply(stream: ParserStream, left: SyntaxInfo, right: SyntaxInfo): SyntaxInfo {
                return AppNode(stream, left, right)
            }

            override fun leftInverse(result: SyntaxInfo): SyntaxInfo? {
                return if (result is AppNode) result.left else null
            }

            override fun rightInverse(result: SyntaxInfo): SyntaxInfo? {
                return if (result is AppNode) result.right else null
            }
        }

        val app = term.then(Reducer.rep(term.annotate(Markers.Arg).fold(appFold))).ref("app")

        expr.set(app)

        parser = expr
    }

    private fun initCstPrinter() {
        this.outStream = StringOutStream()
        this.cstPrinter = object : CstPrinter(outStream) {
            var indent: Int = 0
            var atBeginningOfLine: Boolean = false

            private fun newline() {
                append("\n")
                atBeginningOfLine = true
            }

            override fun print(tree: ConcreteSyntaxTree, annotation: Any): CstPrinter {
                return when (annotation) {
                    Markers.Block -> {
                        newline()
                        indent++
                        print(tree)
                        indent--
                        newline()
                        return this
                    }
                    Markers.Arg -> {
                        append(" ").print(tree)
                    }
                    else -> print(tree)
                }
            }

            override fun print(seq: CharSequence?): CstPrinter {
                if (atBeginningOfLine) {
                    atBeginningOfLine = false
                    append(" ".repeat(indent))
                }

                return append(seq)
            }
        }
    }

    class NumNode(stream: ParserStream, val value: Int) : SyntaxInfo(stream)
    class IdNode(stream: ParserStream, val value: String) : SyntaxInfo(stream)

    class AppNode(stream: ParserStream, val left: SyntaxInfo, val right: SyntaxInfo) : SyntaxInfo(stream)

    enum class Markers { Block, Arg }
}
