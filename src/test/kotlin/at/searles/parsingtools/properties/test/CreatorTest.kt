package at.searles.parsingtools.properties.test

import at.searles.lexer.Lexer
import at.searles.parsing.*
import at.searles.parsingtools.properties.CreatePojo
import at.searles.parsingtools.common.ToString
import at.searles.parsingtools.properties.CreateEmptyProperties
import at.searles.parsingtools.properties.CreateObject
import at.searles.parsingtools.properties.PutProperty
import at.searles.regexparser.StringToRegex
import org.junit.Assert
import org.junit.Test

class CreatorTest {

    private val tokenizer = Lexer()

    private val id = Parser.fromRegex(StringToRegex.parse("[a-z]+"), tokenizer, false, ToString)

    internal val propertiesParser = CreateEmptyProperties.then(
        Recognizer.fromString(",", tokenizer, false).join(
            Recognizer.fromString("+", tokenizer, false).then(id).fold(PutProperty("a"))
                .or(Recognizer.fromString("-", tokenizer, false).then(id).fold(PutProperty("b")), true)
        )
    )

    internal val parser1 = propertiesParser.then(CreateObject<Any>(Item1::class.java, "a", "b"))
    internal val parser2 = propertiesParser.then(CreateObject<Any>(Item2::class.java, true, "a", "b"))
    internal val parser3 = propertiesParser.then(CreatePojo(Item3::class.java))

    private var input: ParserStream? = null
    private var output: String? = null

    @Test
    fun testEmpty1() {
        withInput("")

        val item = actParse(parser1)
        actPrint(parser1, item)

        Assert.assertEquals("", output)
    }

    @Test
    fun testAB1() {
        withInput("+zyx,-wvu")
        val item = actParse(parser1)
        actPrint(parser1, item)

        Assert.assertEquals("+zyx,-wvu", output)
    }

    @Test
    fun testAA1() {
        withInput("+zyx,+wvu")
        val item = actParse(parser1)
        actPrint(parser1, item)

        Assert.assertEquals("+wvu", output)
    }

    @Test
    fun testEmpty2() {
        withInput("")

        val item = actParse(parser2)
        actPrint(parser2, item)

        Assert.assertEquals("", output)
    }

    @Test
    fun testAB2() {
        withInput("+zyx,-wvu")
        val item = actParse(parser2)
        actPrint(parser2, item)

        Assert.assertEquals("+zyx,-wvu", output)
    }

    @Test
    fun testAA2() {
        withInput("+zyx,+wvu")
        val item = actParse(parser2)
        actPrint(parser2, item)

        Assert.assertEquals("+wvu", output)
    }

    @Test
    fun testEmpty3() {
        withInput("")

        val item = actParse(parser3)

        Assert.assertNull(item!!.a)
        Assert.assertNull(item.b)
    }

    @Test
    fun testAB3() {
        withInput("+zyx,-wvu")
        val item = actParse(parser3)

        Assert.assertEquals("zyx", item!!.a)
        Assert.assertEquals("wvu", item.b)
    }

    @Test
    fun testAA3() {
        withInput("+zyx,+wvu")
        val item = actParse(parser3)

        Assert.assertEquals("wvu", item!!.a)
        Assert.assertNull(item.b)
    }

    private fun <T> actPrint(parser: Parser<T>, item: T?) {
        val tree = parser.print(item)
        output = tree?.toString()
    }

    private fun <T> actParse(parser: Parser<T>): T? {
        return parser.parse(input)
    }

    private fun withInput(input: String) {
        this.input = ParserStream.fromString(input)

    }

    class Item1(val a: String?, val b: String?)

    class Item2(stream: ParserStream, val a: String?, val b: String?)

    class Item3 {
        var a: String? = null
        var b: String? = null
    }
}
