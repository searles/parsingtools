package at.searles.parsingtools.properties

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.HashMap

/**
 * This one is for objects with getters and constructors in which
 * all elements are set.
 */
class CreateObject<T>(private val clazz: Class<out T>, private val withInfo: Boolean, vararg val properties: String) :
    Mapping<Properties, T> {

    private val ctor: Constructor<out T>
    private val getters: Map<String, Method>

    init {
        getters = HashMap()
        val parameterTypes = arrayOfNulls<Class<*>>(properties.size + if (withInfo) 1 else 0)

        try {
            var index = 0

            if (withInfo) {
                parameterTypes[index++] = ParserStream::class.java
            }

            for (property in properties) {
                val getter = MethodUtils.getter(clazz, property)
                getters[property] = getter
                parameterTypes[index++] = getter.returnType
            }

            this.ctor = clazz.getConstructor(*parameterTypes)
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException(e)
        }

    }

    constructor(clazz: Class<out T>, vararg properties: String) : this(clazz, false, *properties)

    override fun parse(stream: ParserStream, left: Properties): T? {
        val arguments = arrayOfNulls<Any>(properties.size + if (withInfo) 1 else 0)
        var index = 0

        if (withInfo) {
            arguments[index++] = stream
        }

        for (property in properties) {
            arguments[index++] = left.get<Any>(property)
        }

        try {
            return ctor.newInstance(*arguments)
        } catch (e: InstantiationException) {
            throw IllegalArgumentException(e)
        } catch (e: IllegalAccessException) {
            throw IllegalArgumentException(e)
        } catch (e: InvocationTargetException) {
            throw IllegalArgumentException(e)
        }

    }

    override fun left(result: T): Properties? {
        if (!clazz.isInstance(result)) {
            return null
        }

        try {
            val map = HashMap<String, Any>()

            for (property in properties) {
                val value = getters[property]!!.invoke(result)
                if (value != null) {
                    map[property] = value
                }
            }

            return Properties(map)
        } catch (e: IllegalAccessException) {
            throw IllegalArgumentException(e)
        } catch (e: InvocationTargetException) {
            throw IllegalArgumentException(e)
        }

    }

    override fun toString(): String {
        return "{create}"
    }
}
