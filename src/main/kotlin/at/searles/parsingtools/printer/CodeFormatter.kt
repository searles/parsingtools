package at.searles.parsingtools.printer

import at.searles.buf.Frame
import at.searles.lexer.TokenStream
import at.searles.parsing.ParserStream
import at.searles.regex.CharSet
import at.searles.regex.Regex
import java.lang.Integer.max

class CodeFormatter(val wsTokenId: Int, val editor: Editor): TokenStream.Listener {

    private var indentLevel = 0
    private var indentNext = false

    var forceNewLine = false
    var forceSpace = false

    var indentation = "    "
    var newline = "\n"
    var space = " "

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

    override fun tokenConsumed(src: TokenStream, tokId: Int, frame: Frame) {
        if(tokId == wsTokenId) {
            var nlCount = countNewlines(frame)

            if(forceNewLine) {
                nlCount = max(1, nlCount)
            }

            forceNewLine = false
            forceSpace = false
            indentNext = nlCount > 0

            val replacement = if(nlCount == 0) if(frame.startPosition() == 0L) "" else space else newline.repeat(nlCount)

            editor.edit(frame, replacement)
        } else {
            if(forceNewLine) {
                forceNewLine = false
                forceSpace = false
                indentNext = true
                editor.insert(newline)
            }

            if(forceSpace) {
                forceSpace = false
                editor.insert(space)
            }

            if(indentNext) {
                indentNext = false
                editor.insert(indentation.repeat(indentLevel))
            }

            editor.edit(frame, frame)
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

    companion object {
        val wsRegex: Regex = CharSet.chars('\r'.toInt(), '\n'.toInt(), '\t'.toInt(), ' '.toInt()) // TODO other chars.

        fun countNewlines(chs: CharSequence): Int {
            // TODO others.
            return chs.count { it == '\n' }
        }
    }
}