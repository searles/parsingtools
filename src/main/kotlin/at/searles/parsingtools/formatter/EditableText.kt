package at.searles.parsingtools.formatter

interface EditableText: CharSequence {
    fun replace(start: Long, end: Long, replacement: CharSequence)
    fun insert(position: Long, insertion: CharSequence)
}