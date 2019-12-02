package at.searles.parsingtools.formatter

import at.searles.buf.Frame
import at.searles.lexer.TokenStream
import at.searles.parsing.ParserLookaheadException
import at.searles.parsing.ParserStream
import at.searles.parsing.Recognizable
import java.lang.Integer.max

open class CodeFormatter(private val whiteSpaceTokenId: Int, private val parser: Recognizable) {

    var indentation = "    "
    var newline = "\n"
    var space = " "

    private val indentAnnotations = HashSet<Any?>()
    private val forceNewLineAnnotations = HashSet<Any?>()
    private val forceSpaceAnnotations = HashSet<Any?>()

    fun format(editableText: EditableText): Long {
        val formatterInstance = FormatterInstance(editableText)

        val stream = ParserStream.fromString(editableText)

        stream.tokStream().setListener(formatterInstance)
        stream.setListener(formatterInstance)

        try {
            parser.recognize(stream)
        } catch(e: ParserLookaheadException) {
            // ignore
        }

        formatterInstance.changeRunnables.reversed().forEach { it.run() }

        return formatterInstance.position
    }

    fun addIndentAnnotation(annotation: Any?) {
        this.indentAnnotations.add(annotation)
    }

    fun addForceSpaceAnnotation(annotation: Any?) {
        this.forceSpaceAnnotations.add(annotation)
    }

    fun addForceNewlineAnnotation(annotation: Any?) {
        this.forceNewLineAnnotations.add(annotation)
    }

    private inner class FormatterInstance(val editableText: EditableText): TokenStream.Listener, ParserStream.Listener {
        private var indentLevel = 0
        private var indentNext = false

        private var forceNewLine = false
        private var forceSpace = false

        val changeRunnables = ArrayList<Runnable>()

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

            if (tokenId == whiteSpaceTokenId) {
                var nlCount = countNewlines(frame)

                if (forceNewLine) {
                    nlCount = max(1, nlCount)
                }

                forceNewLine = false
                forceSpace = false
                indentNext = nlCount > 0

                val replacement =
                    if (nlCount == 0) if (frame.startPosition() == 0L) "" else space else newline.repeat(nlCount)

                val start = frame.startPosition()
                val end = frame.endPosition()

                changeRunnables.add(Runnable { editableText.replace(start, end, replacement) })

                position = frame.endPosition()
            } else {
                val currentPosition = position

                if (forceNewLine) {
                    forceNewLine = false
                    forceSpace = false
                    indentNext = true
                    changeRunnables.add(Runnable { editableText.insert(currentPosition, newline) })
                }

                if (forceSpace) {
                    forceSpace = false
                    changeRunnables.add(Runnable { editableText.insert(currentPosition, space) })
                }

                if (indentNext) {
                    indentNext = false

                    if(indentLevel > 0) {
                        val currentIndentation = indentation.repeat(indentLevel)

                        changeRunnables.add(Runnable {
                            editableText.insert(
                                currentPosition,
                                currentIndentation
                            )
                        })
                    }
                }

                position = frame.endPosition()
            }
        }

        override fun <C : Any> annotationBegin(parserStream: ParserStream, annotation: C) {
            if (indentAnnotations.contains(annotation)) {
                indent()
            }
        }

        override fun <C : Any> annotationEnd(parserStream: ParserStream, annotation: C, success: Boolean) {
            if (indentAnnotations.contains(annotation)) {
                unindent()
            }

            if (success) {
                if (forceSpaceAnnotations.contains(annotation)) {
                    forceSpace()
                }

                if (forceNewLineAnnotations.contains(annotation)) {
                    forceNewLine()
                }
            }
        }
    }
}