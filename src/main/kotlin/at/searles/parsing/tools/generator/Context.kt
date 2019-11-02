package at.searles.parsing.tools.generator

import at.searles.lexer.Tokenizer
import at.searles.parsing.*
import at.searles.parsing.tokens.TokenParser
import at.searles.regex.Regex

private fun <T> Reducer<T, T>.opt(): Reducer<T, T> {
    return Reducer.opt(this)
}

private fun <T> Reducer<T, T>.rep(): Reducer<T, T> {
    return Reducer.rep(this)
}

private fun <T> Reducer<T, T>.plus(): Reducer<T, T> {
    return Reducer.rep(this)
}

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
        })
    }

    fun <T> parser(regex: Regex, fn: Mapping<CharSequence, T>): Parser<T> {
        return Parser.fromRegex(regex, tokenizer, true, fn)
    }

    fun text(text: String): Recognizer {
        return Recognizer.fromString(text, tokenizer, false)
    }
}