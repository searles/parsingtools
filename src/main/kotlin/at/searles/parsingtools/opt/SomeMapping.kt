package at.searles.parsingtools.opt

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

import java.util.Optional

/**
 * Created by searles on 02.04.19.
 */
class SomeMapping<T> : Mapping<T, Optional<T>> {
    override fun parse(stream: ParserStream, left: T): Optional<T>? {
        return Optional.of(left)
    }

    override fun left(result: Optional<T>): T? {
        return result.orElse(null)
    }

    override fun toString(): String {
        return "{some}"
    }
}
