package com.episode6.hackit.deployable.extension

import org.gradle.api.Project
import spock.lang.Specification

/**
 * Tests {@link NestedExtension}
 */
class NestedExtensionTest extends Specification {

  Project project
  TestObjectContainer testContainer

  def setup() {
    project = Mock()
    testContainer = new TestObjectContainer(project)
  }

  def "test assignment by equals"() {
    when:
    testContainer.outerTest {
      stringParam = "outerStringParam"
      intParam = 12
      innerTest1 {
        stringParam = "innerStringParam1"
        intParam = 13
        innerInnerTest1 {
          stringParam = "innerInnerStringParam1"
          intParam = 14
        }
        innerInnerTest2 {
          stringParam = "innerInnerStringParam2"
          intParam = 15
        }
      }
      innerTest2 {
        stringParam = "innerStringParam2"
        intParam = 16
        innerInnerTest1 {
          stringParam = "innerInnerStringParam3"
          intParam = 17
        }
        innerInnerTest2 {
          stringParam = "innerInnerStringParam4"
          intParam = 18
        }
      }
    }

    then:
    testContainer.outerTest.stringParam == "outerStringParam"
    testContainer.outerTest.intParam == 12
    testContainer.outerTest.innerTest1.stringParam == "innerStringParam1"
    testContainer.outerTest.innerTest1.intParam == 13
    testContainer.outerTest.innerTest1.innerInnerTest1.stringParam == "innerInnerStringParam1"
    testContainer.outerTest.innerTest1.innerInnerTest1.intParam == 14
    testContainer.outerTest.innerTest1.innerInnerTest2.stringParam == "innerInnerStringParam2"
    testContainer.outerTest.innerTest1.innerInnerTest2.intParam == 15
    testContainer.outerTest.innerTest2.stringParam == "innerStringParam2"
    testContainer.outerTest.innerTest2.intParam == 16
    testContainer.outerTest.innerTest2.innerInnerTest1.stringParam == "innerInnerStringParam3"
    testContainer.outerTest.innerTest2.innerInnerTest1.intParam == 17
    testContainer.outerTest.innerTest2.innerInnerTest2.stringParam == "innerInnerStringParam4"
    testContainer.outerTest.innerTest2.innerInnerTest2.intParam == 18
  }

  def "test assignment by methods"() {
    when:
    testContainer.outerTest {
      stringParam "outerStringParam"
      intParam 12
      innerTest1 {
        stringParam "innerStringParam1"
        intParam 13
        innerInnerTest1 {
          stringParam "innerInnerStringParam1"
          intParam 14
        }
        innerInnerTest2 {
          stringParam "innerInnerStringParam2"
          intParam 15
        }
      }
      innerTest2 {
        stringParam "innerStringParam2"
        intParam 16
        innerInnerTest1 {
          stringParam "innerInnerStringParam3"
          intParam 17
        }
        innerInnerTest2 {
          stringParam "innerInnerStringParam4"
          intParam 18
        }
      }
    }

    then:
    testContainer.outerTest.stringParam == "outerStringParam"
    testContainer.outerTest.intParam == 12
    testContainer.outerTest.innerTest1.stringParam == "innerStringParam1"
    testContainer.outerTest.innerTest1.intParam == 13
    testContainer.outerTest.innerTest1.innerInnerTest1.stringParam == "innerInnerStringParam1"
    testContainer.outerTest.innerTest1.innerInnerTest1.intParam == 14
    testContainer.outerTest.innerTest1.innerInnerTest2.stringParam == "innerInnerStringParam2"
    testContainer.outerTest.innerTest1.innerInnerTest2.intParam == 15
    testContainer.outerTest.innerTest2.stringParam == "innerStringParam2"
    testContainer.outerTest.innerTest2.intParam == 16
    testContainer.outerTest.innerTest2.innerInnerTest1.stringParam == "innerInnerStringParam3"
    testContainer.outerTest.innerTest2.innerInnerTest1.intParam == 17
    testContainer.outerTest.innerTest2.innerInnerTest2.stringParam == "innerInnerStringParam4"
    testContainer.outerTest.innerTest2.innerInnerTest2.intParam == 18
  }

  def "verify project properties queried"() {
    when:
    14 * project.hasProperty(_) >> true
    1 * project.findProperty("outerTest.stringParam") >> "outerStringParam"
    1 * project.findProperty("outerTest.intParam") >> 12
    1 * project.findProperty("outerTest.innerTest1.stringParam") >> "innerStringParam1"
    1 * project.findProperty("outerTest.innerTest1.intParam") >> 13
    1 * project.findProperty("outerTest.innerTest1.innerInnerTest1.stringParam") >> "innerInnerStringParam1"
    1 * project.findProperty("outerTest.innerTest1.innerInnerTest1.intParam") >> 14
    1 * project.findProperty("outerTest.innerTest1.innerInnerTest2.stringParam") >> "innerInnerStringParam2"
    1 * project.findProperty("outerTest.innerTest1.innerInnerTest2.intParam") >> 15
    1 * project.findProperty("outerTest.innerTest2.stringParam") >> "innerStringParam2"
    1 * project.findProperty("outerTest.innerTest2.intParam") >> 16
    1 * project.findProperty("outerTest.innerTest2.innerInnerTest1.stringParam") >> "innerInnerStringParam3"
    1 * project.findProperty("outerTest.innerTest2.innerInnerTest1.intParam") >> 17
    1 * project.findProperty("outerTest.innerTest2.innerInnerTest2.stringParam") >> "innerInnerStringParam4"
    1 * project.findProperty("outerTest.innerTest2.innerInnerTest2.intParam") >> 18

    and:
    String outerStringParam = testContainer.outerTest.stringParam
    Integer outerIntParam = testContainer.outerTest.intParam
    String innerStringParam1 = testContainer.outerTest.innerTest1.stringParam
    Integer innerIntParam1 = testContainer.outerTest.innerTest1.intParam
    String innerInnerStringParam1 = testContainer.outerTest.innerTest1.innerInnerTest1.stringParam
    Integer innerInnerIntParam1 = testContainer.outerTest.innerTest1.innerInnerTest1.intParam
    String innerInnerStringParam2 = testContainer.outerTest.innerTest1.innerInnerTest2.stringParam
    Integer innerInnerIntParam2 = testContainer.outerTest.innerTest1.innerInnerTest2.intParam
    String innerStringParam2 = testContainer.outerTest.innerTest2.stringParam
    Integer innerIntParam2 = testContainer.outerTest.innerTest2.intParam
    String innerInnerStringParam3 = testContainer.outerTest.innerTest2.innerInnerTest1.stringParam
    Integer innerInnerIntParam3 = testContainer.outerTest.innerTest2.innerInnerTest1.intParam
    String innerInnerStringParam4 = testContainer.outerTest.innerTest2.innerInnerTest2.stringParam
    Integer innerInnerIntParam4 = testContainer.outerTest.innerTest2.innerInnerTest2.intParam

    then:
    outerStringParam == "outerStringParam"
    outerIntParam == 12
    innerStringParam1 == "innerStringParam1"
    innerIntParam1 == 13
    innerInnerStringParam1 == "innerInnerStringParam1"
    innerInnerIntParam1 == 14
    innerInnerStringParam2 == "innerInnerStringParam2"
    innerInnerIntParam2 == 15
    innerStringParam2 == "innerStringParam2"
    innerIntParam2 == 16
    innerInnerStringParam3 == "innerInnerStringParam3"
    innerInnerIntParam3 == 17
    innerInnerStringParam4 == "innerInnerStringParam4"
    innerInnerIntParam4 == 18
  }

  def "verify project properties queried via getters"() {
    when:
    14 * project.hasProperty(_) >> true
    1 * project.findProperty("outerTest.stringParam") >> "outerStringParam"
    1 * project.findProperty("outerTest.intParam") >> 12
    1 * project.findProperty("outerTest.innerTest1.stringParam") >> "innerStringParam1"
    1 * project.findProperty("outerTest.innerTest1.intParam") >> 13
    1 * project.findProperty("outerTest.innerTest1.innerInnerTest1.stringParam") >> "innerInnerStringParam1"
    1 * project.findProperty("outerTest.innerTest1.innerInnerTest1.intParam") >> 14
    1 * project.findProperty("outerTest.innerTest1.innerInnerTest2.stringParam") >> "innerInnerStringParam2"
    1 * project.findProperty("outerTest.innerTest1.innerInnerTest2.intParam") >> 15
    1 * project.findProperty("outerTest.innerTest2.stringParam") >> "innerStringParam2"
    1 * project.findProperty("outerTest.innerTest2.intParam") >> 16
    1 * project.findProperty("outerTest.innerTest2.innerInnerTest1.stringParam") >> "innerInnerStringParam3"
    1 * project.findProperty("outerTest.innerTest2.innerInnerTest1.intParam") >> 17
    1 * project.findProperty("outerTest.innerTest2.innerInnerTest2.stringParam") >> "innerInnerStringParam4"
    1 * project.findProperty("outerTest.innerTest2.innerInnerTest2.intParam") >> 18

    and:
    String outerStringParam = testContainer.outerTest.getStringParam()
    Integer outerIntParam = testContainer.outerTest.getIntParam()
    String innerStringParam1 = testContainer.outerTest.innerTest1.getStringParam()
    Integer innerIntParam1 = testContainer.outerTest.innerTest1.getIntParam()
    String innerInnerStringParam1 = testContainer.outerTest.innerTest1.innerInnerTest1.getStringParam()
    Integer innerInnerIntParam1 = testContainer.outerTest.innerTest1.innerInnerTest1.getIntParam()
    String innerInnerStringParam2 = testContainer.outerTest.innerTest1.innerInnerTest2.getStringParam()
    Integer innerInnerIntParam2 = testContainer.outerTest.innerTest1.innerInnerTest2.getIntParam()
    String innerStringParam2 = testContainer.outerTest.innerTest2.getStringParam()
    Integer innerIntParam2 = testContainer.outerTest.innerTest2.getIntParam()
    String innerInnerStringParam3 = testContainer.outerTest.innerTest2.innerInnerTest1.getStringParam()
    Integer innerInnerIntParam3 = testContainer.outerTest.innerTest2.innerInnerTest1.getIntParam()
    String innerInnerStringParam4 = testContainer.outerTest.innerTest2.innerInnerTest2.getStringParam()
    Integer innerInnerIntParam4 = testContainer.outerTest.innerTest2.innerInnerTest2.getIntParam()

    then:
    outerStringParam == "outerStringParam"
    outerIntParam == 12
    innerStringParam1 == "innerStringParam1"
    innerIntParam1 == 13
    innerInnerStringParam1 == "innerInnerStringParam1"
    innerInnerIntParam1 == 14
    innerInnerStringParam2 == "innerInnerStringParam2"
    innerInnerIntParam2 == 15
    innerStringParam2 == "innerStringParam2"
    innerIntParam2 == 16
    innerInnerStringParam3 == "innerInnerStringParam3"
    innerInnerIntParam3 == 17
    innerInnerStringParam4 == "innerInnerStringParam4"
    innerInnerIntParam4 == 18
  }

  static class TestObjectContainer {
    RootExtensionObject outerTest
    TestObjectContainer(Project project) {
      outerTest = new RootExtensionObject(project)
    }

    def outerTest(Closure closure) {
      return outerTest.applyClosure(closure)
    }
  }

  static class RootExtensionObject extends NestedExtension {
    String stringParam
    Integer intParam

    InnerTestClass innerTest1
    InnerTestClass innerTest2

    RootExtensionObject(Project project) {
      super(project, "outerTest")
      innerTest1 = new InnerTestClass(this, "innerTest1")
      innerTest2 = new InnerTestClass(this, "innerTest2")
    }
  }

  static class InnerTestClass extends NestedExtension {
    String stringParam
    Integer intParam
    InnerInnerTestClass innerInnerTest1
    InnerInnerTestClass innerInnerTest2

    InnerTestClass(NestedExtension parent, String newName) {
      super(parent, newName)
      innerInnerTest1 = new InnerInnerTestClass(this, "innerInnerTest1")
      innerInnerTest2 = new InnerInnerTestClass(this, "innerInnerTest2")
    }
  }

  static class InnerInnerTestClass extends NestedExtension {
    String stringParam
    Integer intParam

    InnerInnerTestClass(NestedExtension parent, String newName) {
      super(parent, newName)
    }
  }
}
