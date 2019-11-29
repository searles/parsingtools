package at.searles.parsingtools.printer

import at.searles.buf.Frame
import at.searles.parsing.printing.OutStream

interface DocumentChangeObserver { // FIXME change name!
    fun edit(frame: Frame, replacement: CharSequence)
    fun insert(chs: CharSequence)

    companion object {
        fun fromOutStream(outStream: OutStream): DocumentChangeObserver {
            return object: DocumentChangeObserver {
                override fun edit(frame: Frame, replacement: CharSequence) {
                    outStream.append(replacement)
                }

                override fun insert(chs: CharSequence) {
                    outStream.append(chs)
                }
            }
        }
    }
}
