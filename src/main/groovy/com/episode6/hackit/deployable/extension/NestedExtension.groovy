package com.episode6.hackit.deployable.extension

import org.gradle.api.Project

/**
 * A base class to simplify nested objects in a plugin extension
 * that can be defined either directly, via Closures or via namespaces
 * gradle.properties
 */
abstract class NestedExtension {

  private final Project project
  private final String namespace

  NestedExtension(Project project, String namespace) {
    this.project = project
    this.namespace = namespace
  }

  NestedExtension(Project project, String parentNamespace, String newName) {
    this(project, "${parentNamespace}.${newName}")
  }

  NestedExtension(NestedExtension parent, String newName) {
    this(parent.getProject(), parent.getNamespace(), newName)
  }

  /**
   * Magic method handles using prop names as setter methods (passing
   * either a String or a Closure)
   */
  @Override
  Object invokeMethod(String name, Object args) {
    if (hasProperty(name) && args instanceof Object[] && args.length == 1) {
      Object arg = args[0]
      if (arg instanceof Closure) {
        Object propertyValue = metaClass.getProperty(this, name)
        if (propertyValue instanceof NestedExtension) {
          return propertyValue.applyClosure(arg)
        }
      } else {
        metaClass.setProperty(this, name, arg)
        return
      }
    }

    throw new MissingMethodException(name, this.getClass(), args, false)
  }

  /**
   * Magic method handles getting of properties. If the local property
   * is null, we check for a matching, fully-qualified project property.
   * If neither if found, we through (except when the property's get method
   * is explicitly overridden.
   */
  @Override
  Object getProperty(String propName) {
    Object obj = metaClass.getProperty(this, propName)
    if (obj instanceof NestedExtension || obj != null) {
      return obj
    }

    return getOptionalProjectProperty(propName)
  }

  /**
   * apply a given closure to $this
   */
  Object applyClosure(Closure closure) {
    closure.setDelegate(this)
    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
    closure.call()
    return this
  }

  List<String> findMissingProps() {
    List<String> missingProps = new LinkedList<>()
    getProperties().keySet().each { key ->
      // explicitly call getProperty so we check getOptionalProjectProperty as well
      Object value = getProperty(key)
      if (value == null) {
        missingProps.add(qualifyPropertyName(key))
      } else if (value instanceof NestedExtension) {
        missingProps.addAll(value.findMissingProps())
      }
    }
    return missingProps
  }

  Project getProject() {
    return project
  }

  String getNamespace() {
    return namespace
  }

  protected Object getOptionalProjectProperty(String propertyName) {
    String fullyQualifiedPropertyName = qualifyPropertyName(propertyName)
    if (project.hasProperty(fullyQualifiedPropertyName)) {
      return project.findProperty(fullyQualifiedPropertyName)
    }
    return null
  }

  protected String qualifyPropertyName(String propertyName) {
    return "${namespace}.${propertyName}"
  }
}

