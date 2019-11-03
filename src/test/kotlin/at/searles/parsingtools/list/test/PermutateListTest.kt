package at.searles.parsingtools.list.test

import at.searles.parsing.ParserStream
import at.searles.parsingtools.list.PermutateList
import org.junit.Assert
import org.junit.Test

import java.util.Arrays

class PermutateListTest {

    @Test
    fun parse() {
        val mapping = PermutateList<String>(1, 2, 0)

        val result = mapping.parse(ParserStream.fromString(""), listOf("A", "B", "C"))

        Assert.assertEquals(listOf("B", "C", "A"), result)
    }

    @Test
    fun left() {
        val mapping = PermutateList<String>(1, 2, 0)

        val result = mapping.left(listOf("A", "B", "C"))

        Assert.assertEquals(listOf("C", "A", "B"), result)
    }
}