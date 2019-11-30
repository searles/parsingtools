package at.searles.parsingtools.printer

import at.searles.buf.Frame
import at.searles.parsing.printing.OutStream

interface DocumentChangeObserver {
    fun edit(frame: Frame, replacement: CharSequence)
    fun insert(position: Long, chs: CharSequence)

    companion object {
        fun fromOutStream(outStream: OutStream): DocumentChangeObserver {
            return object: DocumentChangeObserver {
                /**
                 * If frame == replacement, no change is needed
                 */
                override fun edit(frame: Frame, replacement: CharSequence) {
                    outStream.append(replacement)
                }

                override fun insert(position: Long, chs: CharSequence) {
                    outStream.append(chs)
                }
            }
        }
    }
}
