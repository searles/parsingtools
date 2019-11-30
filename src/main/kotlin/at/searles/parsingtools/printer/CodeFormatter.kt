package at.searles.parsingtools.printer

import at.searles.buf.Frame
import at.searles.lexer.TokenStream
import at.searles.parsing.ParserStream
import java.lang.Integer.max

class CodeFormatter(private val whiteSpaceTokenId: Int, private val documentChangeObserver: DocumentChangeObserver): TokenStream.Listener {

    var indentation = "    "
    var newline = "\n"
    var space = " "

    private var indentLevel = 0
    private var indentNext = false

    private var forceNewLine = false
    private var forceSpace = false

    var position: Long = 0
        private set

    fun forceNewLine() {
        forceNewLine = true
    }

    fun forceSpace() {
        forceSpace = true
    }

    fun indent() {
        indentLevel++
    }

    fun unindent() {
        indentLevel--
    }

    private fun countNewlines(chs: CharSequence): Int {
        return chs.count { it == '\n' }
    }

    override fun tokenConsumed(src: TokenStream, tokenId: Int, frame: Frame) {
        require(position == frame.startPosition())

        if(tokenId == whiteSpaceTokenId) {
            var nlCount = countNewlines(frame)

            if(forceNewLine) {
                nlCount = max(1, nlCount)
            }

            forceNewLine = false
            forceSpace = false
            indentNext = nlCount > 0

            val replacement = if(nlCount == 0) if(frame.startPosition() == 0L) "" else space else newline.repeat(nlCount)

            documentChangeObserver.edit(frame, replacement)

            position = frame.endPosition()
        } else {
            if(forceNewLine) {
                forceNewLine = false
                forceSpace = false
                indentNext = true
                documentChangeObserver.insert(position, newline)
            }

            if(forceSpace) {
                forceSpace = false
                documentChangeObserver.insert(position, space)
            }

            if(indentNext) {
                indentNext = false
                documentChangeObserver.insert(position, indentation.repeat(indentLevel))
            }

            documentChangeObserver.edit(frame, frame)

            position = frame.endPosition()
        }
    }


    fun createParserStreamListener(indentAnnotations: Set<Any>, forceSpaceAnnotations: Set<Any> = emptySet(), forceNewLineAnnotations: Set<Any> = emptySet()): ParserStream.Listener {
        return object : ParserStream.Listener {
            override fun <C : Any> annotationBegin(parserStream: ParserStream, annotation: C) {
                if(indentAnnotations.contains(annotation)) {
                    indent()
                }
            }

            override fun <C : Any> annotationEnd(parserStream: ParserStream, annotation: C, success: Boolean) {
                if(indentAnnotations.contains(annotation)) {
                    unindent()
                }

                if(success) {
                    if(forceSpaceAnnotations.contains(annotation)) {
                        forceSpace()
                    }

                    if(forceNewLineAnnotations.contains(annotation)) {
                        forceNewLine()
                    }
                }
            }
        }
    }
}