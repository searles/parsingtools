package at.searles.parsingtools

import at.searles.parsing.*
import at.searles.parsingtools.properties.*
import at.searles.parsingtools.common.SwapPairFold
import at.searles.parsingtools.list.*
import at.searles.parsingtools.common.PairFold
import at.searles.parsingtools.common.ValueInitializer
import at.searles.parsingtools.map.EmptyMap
import at.searles.parsingtools.map.PutFold
import at.searles.parsingtools.map.SingleMap
import at.searles.parsingtools.opt.NoneInitializer
import at.searles.parsingtools.opt.SomeMapping
import at.searles.utils.Pair
import java.util.Optional

/**
 * This class contains utilities to create lists out of parsers for convenience.
 */
object Utils {

    /**
     * Parser for a separated list.
     *
     * @param <T>       The base type.
     * @param separator The separator, eg a comma
     * @param parser    The parser for all elements
     * @return An invertible parser for a list of items.
    </T> */
    fun <T> list1(parser: Parser<T>, separator: Recognizer): Parser<List<T>> {
        // XXX there is also joinPlus!
        return singleton(parser).then(
            Reducer.rep(separator.then(append(parser, 1)))
        )
    }

    fun <T> list1(parser: Parser<T>): Parser<List<T>> {
        // XXX there is also joinPlus.
        return singleton(parser).then(Reducer.rep(append(parser, 1)))
    }

    fun <T> list(parser: Parser<T>, separator: Recognizer): Parser<List<T>> {
        return Utils.empty<T>().then(separator.join(append(parser, 0)))
    }

    fun <T> list(parser: Parser<T>): Parser<List<T>> {
        return Utils.empty<T>().then(Reducer.rep(append(parser, 0)))
    }

    fun <T> empty(): Initializer<List<T>> {
        return EmptyList()
    }

    fun <T> singleton(parser: Parser<T>): Parser<List<T>> {
        return parser.then(SingleList())
    }

    fun <T> binary(rightParser: Parser<T>): Reducer<T, List<T>> {
        return rightParser.fold(BinaryList())
    }

    /**
     * Creates a reducer that appends a parsed element to the left list
     *
     * @param minLeftElements The minimum number of elements that are asserted to be in the left list. This is
     * needed for inversion.
     */
    fun <T> append(parser: Parser<T>, minLeftElements: Int): Reducer<List<T>, List<T>> {
        return parser.fold(AddList(minLeftElements))
    }

    /**
     * Use this to swap elements in the incoming list. If the lhs list contains
     * the elements "A", "B", "C" in this order and 'order' is 1, 2, 0, then
     * the returned list will be "B", "C", "A". Make sure to provide a meaningful
     * order, otherwise, the return value is undetermined but most likely an exception.
     */
    fun <T> permutate(vararg order: Int): Mapping<List<T>, List<T>> {
        return PermutateList(*order)
    }

    fun <T> opt(parser: Parser<T>): Parser<Optional<T>> {
        return parser.then(SomeMapping()).or(NoneInitializer())
    }

    fun <T, U> pair(rightParser: Parser<U>): Reducer<T, Pair<T, U>> {
        return rightParser.fold(PairFold())
    }

    fun <T, U> swapPair(leftParser: Parser<U>): Reducer<T, Pair<U, T>> {
        return leftParser.fold(SwapPairFold())
    }

    /**
     * @return An initializer for a simple value.
     */
    fun <V> `val`(v: V): Initializer<V> {
        return ValueInitializer(v)
    }

    // Add things to a map

    fun <K, V> map(): Initializer<Map<K, V>> {
        return EmptyMap()
    }

    fun <K, V> map(key: K, parser: Parser<V>): Parser<Map<K, V>> {
        return parser.then(SingleMap<K, V>(key))
    }

    fun <K, V> put(key: K, itemParser: Parser<V>): Reducer<Map<K, V>, Map<K, V>> {
        return itemParser.fold(PutFold<K, V>(key))
    }

    // === generic classes ===

    /**
     * Creates an empty builder
     */
    fun properties(): Initializer<Properties> {
        return PropertiesInitializer()
    }

    fun <T> properties(id: String): Mapping<T, Properties> {
        return PropertiesSingleton(id)
    }

    fun <T> put(property: String, parser: Parser<T>): Reducer<Properties, Properties> {
        return parser.fold(PropertyPut(property))
    }

    fun <T> create(clazz: Class<out T>, vararg properties: String): Mapping<Properties, T> {
        return Creator(clazz, *properties)
    }

    fun <T> create(clazz: Class<out T>, withInfo: Boolean, vararg properties: String): Mapping<Properties, T> {
        return Creator(clazz, withInfo, *properties)
    }

}
