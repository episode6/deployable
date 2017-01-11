package com.episode6.hackit.deployable.extension

import org.gradle.api.Project

/**
 * A base class to simplify nested objects in a plugin extension
 * that can be defined either directly or via Closures
 */
abstract class NestedExtension {

  protected final Project project
  protected final String namespace

  NestedExtension(Project project, String namespace) {
    this.project = project
    this.namespace = namespace
  }

  NestedExtension(Project project, String parentNamespace, String newName) {
    this(project, "${parentNamespace}.${newName}")
  }

  /**
   * Magic method handles using prop names as setter methods (passing
   * either a String or a Closure)
   */
  @Override
  Object invokeMethod(String name, Object args) {
    if (hasProperty(name) && args instanceof Object[] && ((Object[])args).length == 1) {
      Object arg = ((Object[])args)[0]
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

  /**
   * apply a given closure to $this
   */
  def applyClosure(Closure closure) {
    closure.setDelegate(this)
    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
    closure.call()
    return this
  }
}

