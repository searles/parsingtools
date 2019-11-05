package at.searles.parsingtools.properties

import java.lang.reflect.Method

internal object MethodUtils {
    private fun methodAccess(prefix: String, property: String): String {
        return prefix + property.substring(0, 1).toUpperCase() + property.substring(1)
    }

    @Throws(NoSuchMethodException::class)
    fun setter(clazz: Class<*>, propertyName: String): Method {
        return clazz.getMethod(methodAccess("set", propertyName), getter(clazz, propertyName).returnType)
    }

    @Throws(NoSuchMethodException::class)
    fun getter(clazz: Class<*>, propertyName: String): Method {
        return clazz.getMethod(methodAccess("get", propertyName))
    }
}
