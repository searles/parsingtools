package at.searles.parsingtools.properties.test

import at.searles.lexer.Lexer
import at.searles.parsing.*
import at.searles.parsingtools.*
import at.searles.parsingtools.properties.CreateEmptyProperties
import at.searles.parsingtools.properties.CreateObject
import at.searles.parsingtools.properties.CreateSingletonProperties
import at.searles.regexparser.StringToRegex
import org.junit.Assert
import org.junit.Test

class CreatorPutTest {
    private val tokenizer = Lexer()
    private val id = Parser.fromRegex(StringToRegex.parse("[a-z]+"),
        tokenizer, false,
        object : Mapping<CharSequence, Any> {
            override fun parse(stream: ParserStream, left: CharSequence): Any? {
                return left.toString()
            }

            override fun left(result: Any): CharSequence? {
                return if (result is String) result.toString() else null
            }
        })

    private val parser = id.then(
            Recognizer.fromString("+", tokenizer, false)
                .then(CreateSingletonProperties<Any>("a"))
                .then(CreateObject<Any>(Item::class.java, "a"))
            .opt()
    )
    private var input: ParserStream? = null
    private var item: Any? = null // using object to test inheritance
    private var output: String? = null

    @Test
    fun testNoOpt() {
        withInput("k")
        actParse()

        Assert.assertTrue(item is String)
    }

    @Test
    fun testOpt() {
        withInput("k+")
        actParse()

        Assert.assertTrue(item is Item)
    }

    @Test
    fun testOptPrint() {
        withInput("k+")
        actParse()
        actPrint()

        Assert.assertEquals("k+", output)
    }

    @Test
    fun testNoOptPrint() {
        withInput("k")
        actParse()
        actPrint()

        Assert.assertEquals("k", output)
    }

    private fun actPrint() {
        val tree = parser.print(item)
        output = tree?.toString()
    }

    private fun actParse() {
        item = parser.parse(input)
    }

    private fun withInput(input: String) {
        this.input = ParserStream.fromString(input)

    }

    class Item(val a: String)
}
