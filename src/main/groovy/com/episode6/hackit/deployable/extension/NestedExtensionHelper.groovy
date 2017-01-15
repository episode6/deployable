package com.episode6.hackit.deployable.extension

/**
 * Helper class to translate directly-accessed get methods into calls to getProperty() so that
 * the gradle.properties can be checked
 */
class NestedExtensionHelper {

  static final List<String> OBJECT_METHODS = GroovyObject.metaClass.getMetaMethods().collect {it.name}
  static final List<String> SPECIAL_GET_METHODS = [
      "getProperty",
      "getProject",
      "getNamespace",
  ]

  static Object handleRealMethod(NestedExtension extension, MetaMethod method, Object args) {
    String propName = getPropertyNameFromGetterMethod(method)
    if (propName == null || !extension.hasProperty(propName)) {
      return method.invoke(extension, args)
    }
    return extension.getProperty(propName)
  }

  static String getPropertyNameFromGetterMethod(MetaMethod method) {
    if (method.name == null ||
        method.name.length() <= 3 ||
        method.isProtected() ||
        !method.name.startsWith("get") ||
        OBJECT_METHODS.contains(method.name) ||
        SPECIAL_GET_METHODS.contains(method.name)) {
      return null
    }

    String propName = method.name.substring(3)
    return propName.substring(0, 1).toLowerCase() + propName.substring(1)
  }
}
