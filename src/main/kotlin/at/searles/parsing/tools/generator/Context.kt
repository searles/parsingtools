package at.searles.parsing.tools.generator

import at.searles.lexer.Tokenizer
import at.searles.parsing.Parser
import at.searles.parsing.Recognizer
import at.searles.parsing.tokens.TokenParser
import at.searles.regex.Regex

class Context(val tokenizer: Tokenizer) {
    fun parser(regex: Regex): TokenParser {
        val id = tokenizer.add(regex)
        return TokenParser(id, tokenizer, true)
    }

    fun <T> parser(regex: Regex, fn: (CharSequence) -> T): Parser<T> {
        return Parser.fromRegex(regex, tokenizer, true, { _, seq -> fn(seq) })
    }

    fun text(text: String): Recognizer {
        return Recognizer.fromString(text, tokenizer, false)
    }
}