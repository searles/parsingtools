package at.searles.parsingtools.list

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

import java.util.ArrayList

class PermutateList<T>(private vararg val order: Int) : Mapping<List<T>, List<T>> {

    override fun parse(stream: ParserStream, left: List<T>): List<T>? {
        val list = ArrayList<T>(left.size)

        for (index in order) {
            list.add(left[index])
        }

        return list
    }

    override fun left(result: List<T>): List<T>? {
        if (result.size != order.size) {
            return null
        }

        val list = ArrayList<T>(result)

        for (i in order.indices) {
            list[order[i]] = result[i]
        }

        return list
    }
}
