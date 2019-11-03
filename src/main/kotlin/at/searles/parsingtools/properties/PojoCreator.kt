package at.searles.parsingtools.properties

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Creates an object with setters and getters but without knowing
 * the parameters. No inversion thus.
 */
class PojoCreator<T> @JvmOverloads constructor(clazz: Class<T>, withInfo: Boolean = false) : Mapping<Properties, T> {

    private var ctor: Constructor<T>? = null
    private var clazz: Class<T>? = null

    init {
        try {
            this.ctor = if (withInfo) clazz.getConstructor(ParserStream::class.java) else clazz.getConstructor()
            this.clazz = clazz
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException(e)
        }

    }

    override fun parse(stream: ParserStream, left: Properties): T? {
        try {
            val obj = if (ctor!!.parameterCount == 0) ctor!!.newInstance() else ctor!!.newInstance(stream)

            for (key in left) {
                val setter = MethodUtils.setter(clazz!!, key)
                setter.invoke(obj, left[key])
            }

            return obj
        } catch (e: InstantiationException) {
            throw IllegalArgumentException(e)
        } catch (e: IllegalAccessException) {
            throw IllegalArgumentException(e)
        } catch (e: InvocationTargetException) {
            throw IllegalArgumentException(e)
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException(e)
        }

    }
}
