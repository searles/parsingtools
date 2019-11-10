package at.searles.parsingtools.printer.test

import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.*
import at.searles.parsing.printing.*
import at.searles.parsingtools.printer.CodeFormatter
import at.searles.parsingtools.printer.Editor
import at.searles.regexparser.StringToRegex
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FormatterTest {

    @Before
    fun setUp() {
        initParser()
    }

    @Test
    fun constantFormatTest() {
        initParser()
        withInput("(a)")
        actFormat()

        Assert.assertEquals("(\n    a\n)", output)
    }

    @Test
    fun appFormatTest() {
        initParser()
        withInput("a(b)c")
        actFormat()

        Assert.assertEquals("a (\n" +
                "    b\n" +
                ")\n" +
                "c", output)
    }

    @Test
    fun constantFormatWithSpaceTest() {
        initParser()
        withInput("( a)")
        actFormat()

        Assert.assertEquals("(\n    a\n)", output)
    }

    @Test
    fun appInAppFormatTest() {
        initParser()
        withInput("a ( b\n c )")
        actFormat()

        Assert.assertEquals("a (\n" +
                "    b\n" +
                "    c \n" +
                ")", output)
    }

    @Test
    fun manyEmbeddedAppsFormatTest() {
        initParser()
        withInput("a (\n b ( c d\n (e (f g h(i \n\nj) k (l \nm n)\n )\n ) o\n p (\nq r (\ns t)\n)\n)\n)\n\n")
        actFormat()

        Assert.assertEquals("a (\n" +
                "    b (\n" +
                "        c d\n" +
                "        (\n" +
                "            e (\n" +
                "                f g h (\n" +
                "                    i\n" +
                "\n" +
                "                    j\n" +
                "                )\n" +
                "                k (\n" +
                "                    l\n" +
                "                    m n\n" +
                "                )\n" +
                "\n" +
                "            )\n" +
                "\n" +
                "        )\n" +
                "        o\n" +
                "        p (\n" +
                "            q r (\n" +
                "                s t\n" +
                "            )\n" +
                "\n" +
                "        )\n" +
                "\n" +
                "    )\n" +
                "\n" +
                ")\n" +
                "\n", output)
    }

    private fun withInput(input: String) {
        this.inStream = ParserStream.fromString(input)
    }

    private fun actFormat() {
        outStream = StringOutStream()

        val formatter = CodeFormatter(whiteSpaceTokId, Editor.fromOutStream(outStream))

        inStream.tokStream().setListener(formatter)
        inStream.setListener(formatter.createParserStreamListener(
            setOf(Markers.Block),
            setOf(Markers.SpaceAfter),
            setOf(Markers.NewlineAfter)

        ))

        Assert.assertTrue(parser.recognize(inStream))
        output = outStream.toString()
    }

    private var whiteSpaceTokId: Int = Integer.MIN_VALUE // invalid default value.
    private lateinit var outStream: StringOutStream
    private lateinit var inStream: ParserStream
    private lateinit var parser: Parser<Node>
    private var output: String? = null

    private fun initParser() {
        val lexer = Lexer()
        val tokenizer = SkipTokenizer(lexer)

        whiteSpaceTokId = lexer.add(StringToRegex.parse("[ \n\r\t]+"))
        tokenizer.addSkipped(whiteSpaceTokId)

        val openPar = Recognizer.fromString("(", tokenizer, false)
        val closePar = Recognizer.fromString(")", tokenizer, false)

        val idMapping = object : Mapping<CharSequence, Node> {
            override fun parse(stream: ParserStream, left: CharSequence): Node =
                IdNode(stream.createTrace(), left.toString())

            override fun left(result: Node): CharSequence? =
                    if (result is IdNode) result.value else null
        }

        val numMapping = object : Mapping<CharSequence, Node> {
            override fun parse(stream: ParserStream, left: CharSequence): Node =
                NumNode(
                    stream.createTrace(),
                    Integer.parseInt(left.toString())
                )

            override fun left(result: Node): CharSequence? =
                    if (result is NumNode) result.value.toString() else null
        }

        val id = Parser.fromToken(lexer.add(StringToRegex.parse("[a-z]+")), tokenizer, false, idMapping).ref("id")
        val num = Parser.fromToken(lexer.add(StringToRegex.parse("[0-9]+")), tokenizer, false, numMapping).ref("num")

        val expr = Ref<Node>("expr")

        // term = id | num | '(' expr ')'
        val term = id.or(num).or(
            openPar.annotate(Markers.NewlineAfter)
                .then(expr.annotate(Markers.Block).annotate(Markers.NewlineAfter))
                .then(closePar.annotate(Markers.NewlineAfter))
        ).annotate(Markers.SpaceAfter)

        // app = term+
        val appFold = object : Fold<Node, Node, Node> {
            override fun apply(stream: ParserStream, left: Node, right: Node): Node {
                return AppNode(stream.createTrace(), left, right)
            }

            override fun leftInverse(result: Node): Node? {
                return if (result is AppNode) result.left else null
            }

            override fun rightInverse(result: Node): Node? {
                return if (result is AppNode) result.right else null
            }
        }

        val app = term.then(Reducer.rep(term.fold(appFold))).ref("app")

        expr.set(app)

        parser = expr
    }

    abstract class Node(val trace: Trace)

    class NumNode(trace: Trace, val value: Int) : Node(trace)
    class IdNode(trace: Trace, val value: String) : Node(trace)

    class AppNode(trace: Trace, val left: Node, val right: Node) : Node(trace)

    enum class Markers { Block, SpaceAfter, NewlineAfter }
}
