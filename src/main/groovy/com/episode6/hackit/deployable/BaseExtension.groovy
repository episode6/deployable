package com.episode6.hackit.deployable

import org.gradle.api.Project

/**
 *
 */
class BaseExtension {

  protected final Project project
  protected final String namespace

  BaseExtension(Project project, String parentNamespace, String newName) {
    this.project = project
    this.namespace = "${parentNamespace}.${newName}"
  }

  @Override
  Object invokeMethod(String name, Object args) {
    System.out.println("unknown method: ${name}")
    if (hasProperty(name) && args instanceof Object[] && ((Object[])args).length == 1) {
      Object arg = ((Object[])args)[0]
      System.out.println("hasProperty: ${name} with arg: ${arg.getClass().getSimpleName()}")
      if (arg instanceof Closure) {
        Object propertyValue = metaClass.getProperty(this, name)
        if (propertyValue instanceof BaseExtension) {
          return propertyValue.applyClosure(arg)
        }
      }
      if (arg instanceof String) {
        metaClass.setProperty(this, name, arg)
        return
      }
    }

    throw new MissingMethodException(name, this.getClass(), args, false)
  }

  @Override
  Object getProperty(String propName) {
    Object obj = metaClass.getProperty(this, propName)
    if (obj instanceof BaseExtension || obj != null || propName == "namespace" || propName == "project") {
      return obj
    }

    return getProjectPropertyOrThrow(propName)
  }

  protected Object getOptionalProjectProperty(String propertyName) {
    String fullyQualifiedPropertyName = qualifyPropertyName(propertyName)
    if (project.hasProperty(fullyQualifiedPropertyName)) {
      return project.findProperty(fullyQualifiedPropertyName)
    }
    return null
  }

  protected Object getProjectPropertyOrThrow(String propertyName) {
    String fullyQualifiedPropertyName = qualifyPropertyName(propertyName)
    if (project.hasProperty(fullyQualifiedPropertyName)) {
      return project.findProperty(fullyQualifiedPropertyName)
    }
    throw new RuntimeException("No value found for property: ${fullyQualifiedPropertyName}")
  }

  protected String qualifyPropertyName(String propertyName) {
    return "${namespace}.${propertyName}"
  }

  def applyClosure(Closure closure) {
    closure.setDelegate(this)
    closure.setResolveStrategy(Closure.DELEGATE_ONLY)
    closure.call()
    return this
  }
}

