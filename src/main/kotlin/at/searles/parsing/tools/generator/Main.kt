package at.searles.parsing.tools.generator

import at.searles.buf.ReaderCharStream
import at.searles.lexer.TokenStream
import at.searles.parsing.ParserStream
import java.io.FileReader

object Main {
    fun main(args: Array<String>) {
        val stream = ParserStream(TokenStream.fromCharStream(ReaderCharStream(FileReader(args[0]))))
        val result = Generator.program.parse(stream)

        if(result == null) {
            error("Could not generate code")
        } else {
            val visitor = KotlinVisitor()
            print(result.accept(visitor))
        }
    }
}