import at.searles.parsing.ParserStream
import at.searles.parsing.tools.generator.Generator
import at.searles.parsing.utils.ast.AstNode
import org.junit.Assert
import org.junit.Test

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

    private fun parseExpr(): AstNode? {
        return Generator.expr.parse(parserStream)
    }

    private fun withInput(input: String) {
        parserStream = ParserStream.fromString(input)
    }
}