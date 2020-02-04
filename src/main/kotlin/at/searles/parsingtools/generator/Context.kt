package at.searles.parsingtools.generator

import at.searles.lexer.Tokenizer
import at.searles.parsing.*
import at.searles.parsing.tokens.TokenParser
import at.searles.regex.Regex

class Context(val tokenizer: Tokenizer) {
    fun parser(regex: Regex): TokenParser {
        val id = tokenizer.add(regex)
        return TokenParser(id, tokenizer, true)
    }

    fun <T> parser(regex: Regex, fn: (CharSequence) -> T): Parser<T> {
        return Parser.fromRegex(regex, tokenizer, true, object: Mapping<CharSequence, T> {
            override fun parse(stream: ParserStream, left: CharSequence): T {
                return fn(left)
            }

            override fun left(result: T): CharSequence? {
                return result.toString()
            }

            override fun toString(): String {
                return regex.toString()
            }
        })
    }

    fun <T> parser(regex: Regex, fn: Mapping<CharSequence, T>): Parser<T> {
        return Parser.fromRegex(regex, tokenizer, true, fn)
    }

    fun text(text: String): Recognizer {
        return Recognizer.fromString(text, tokenizer, false)
    }
}