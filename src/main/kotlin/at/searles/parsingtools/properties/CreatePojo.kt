package at.searles.parsingtools.properties

import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream
import at.searles.parsing.Trace

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

/**
 * Creates an object with setters and getters but without knowing
 * the parameters. No inversion thus.
 */
class CreatePojo<T> constructor(private val clazz: Class<T>, private val withTrace: Boolean = false) : Mapping<Properties, T> {

    private val ctor: Constructor<T>

    init {
        try {
            this.ctor = if (withTrace) clazz.getConstructor(Trace::class.java) else clazz.getConstructor()
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException(e)
        }

    }

    override fun parse(stream: ParserStream, left: Properties): T? {
        try {
            val obj = if (withTrace) ctor.newInstance(stream.createTrace()) else ctor.newInstance()

            for (key in left) {
                val setter = MethodUtils.setter(clazz, key)
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
