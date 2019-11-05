import at.searles.buf.ReaderCharStream
import at.searles.lexer.TokenStream
import at.searles.parsing.ParserStream
import at.searles.parsingtools.generator.Generator
import at.searles.parsingtools.SyntaxInfo
import at.searles.parsingtools.generator.KotlinVisitor
import org.junit.Assert
import org.junit.Test
import java.io.FileReader

class GeneratorTest {
    private lateinit var parserStream: ParserStream

    @Test
    fun simpleChar() {
        withInput("'a'")
        val output = parseExpr()

        if(output is Generator.StringNode) {
            Assert.assertEquals("a", output.string)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun rawText() {
        withInput("'abc\\\\\\''")
        val output = parseExpr()

        if(output is Generator.StringNode) {
            Assert.assertEquals("abc\\'", output.string)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun escString() {
        withInput("\"abc\\n\\t\\r\\!\\x30\\U00000031\\u0032\"")
        val output = parseExpr()

        if(output is Generator.StringNode) {
            Assert.assertEquals("abc\n\t\r!012", output.string)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun longCodeTest() {
        withInput("{{{ this is some java code I guess... // HELP }}}")
        val output = parseExpr()

        if(output is Generator.CodeNode) {
            Assert.assertEquals(" this is some java code I guess... // HELP ", output.code)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun codeTest() {
        withInput("`just testing this here.`")
        val output = parseExpr()

        if(output is Generator.CodeNode) {
            Assert.assertEquals("just testing this here.", output.code)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun concatTest() {
        withInput("a b")
        val output = parseExpr()

        Assert.assertTrue(output is Generator.ConcatNode)
    }

    @Test
    fun simpleCharSetTest() {
        withInput("[0-9]")
        val output = parseExpr()

        Assert.assertTrue(output is Generator.CharSetNode)
    }

    @Test
    fun regexString() {
        val input = ParserStream.fromString("regex('a',`a`)")
        val output = Generator.RegexParser.parse(input)

        Assert.assertTrue(output is Generator.RegexParserNode)
    }

    @Test
    fun arithmetics() {
        val input = ParserStream(TokenStream.fromCharStream(ReaderCharStream(FileReader("src/test/resources/arithmetics.grammar"))))
        val output = Generator.program.parse(input)


        if(output is Generator.Program) {
            val kotlinSource = output.accept(KotlinVisitor())
            Assert.assertEquals("Arithmetics", output.grammar.name)
            //File("src/test/kotlin/at/searles/parsing/tools/generator/test/Arithmetics.kt").writeText(kotlinSource)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun meelan() {
        val input = ParserStream(TokenStream.fromCharStream(ReaderCharStream(FileReader("src/test/resources/Meelan.grammar"))))
        val output = Generator.program.parse(input)


        if(output is Generator.Program) {
            val kotlinSource = output.accept(KotlinVisitor())
            //File("src/test/kotlin/Meelan.kt").writeText(kotlinSource)
        } else {
            Assert.fail()
        }
    }

    private fun parseExpr(): SyntaxInfo? {
        return Generator.expr.parse(parserStream)
    }

    private fun withInput(input: String) {
        parserStream = ParserStream.fromString(input)
    }
}