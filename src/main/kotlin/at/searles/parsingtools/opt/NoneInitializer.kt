package at.searles.parsingtools.opt

import at.searles.parsing.Initializer
import at.searles.parsing.ParserStream
import java.util.Optional

/**
 * Created by searles on 02.04.19.
 */
class NoneInitializer<T> : Initializer<Optional<T>> {
    override fun parse(stream: ParserStream): Optional<T>? {
        return Optional.empty()
    }

    override fun consume(t: Optional<T>): Boolean {
        return !t.isPresent
    }

    override fun toString(): String {
        return "{none}"
    }
}
