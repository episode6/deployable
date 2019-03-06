Deployable for Gradle
=====================
Gradle plugins to ease the pain of deploying jars and aars to maven repositories.

## Setup
Add Deployable to the classpath in your root `build.gradle`
```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.episode6.hackit.deployable:deployable:0.2.3'
  }
}
```

Set up the `group` and `version` variables
```groovy
allprojects {
  group = "com.example.mygroup"
  version = "1.0-SNAPSHOT"
}
```

In each deployable sub-module apply one of the plugins to `build.gradle`
```groovy
// to deploy a JAR
apply plugin: 'com.episode6.hackit.deployable.jar'

// to deploy an Android AAR
apply plugin: 'com.episode6.hackit.deployable.aar'

// to deploy a JAR with kotlin support
// (requires org.jetbrains.dokka:dokka-gradle-plugin on the classpath)
apply plugin: 'com.episode6.hackit.deployable.kt.jar'

// to deploy an Android AAR with kotlin support
// (requires org.jetbrains.dokka:dokka-android-gradle-plugin on the classpath)
apply plugin: 'com.episode6.hackit.deployable.kt.aar'
```

#### Kotlin Setup
If you need kotlin support, you must also include dokka on your buildscript classpath (for javadoc support) and use one of the `kt` plugins instead
```groovy
// deployable JAR with Kotlin support

buildscript {
  repositories { jcenter() }
  dependencies {
    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    classpath "com.episode6.hackit.deployable:deployable:$deployableVersion"
    classpath "org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion"
  }
}

apply plugin: 'kotlin'
apply plugin: 'com.episode6.hackit.deployable.kt.jar'
```

Similarly for a kotlin-android library...
```groovy
// deployable AAR with Kotlin support

buildscript {
  repositories {
    jcenter()
    google()
  }
  dependencies {
    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    classpath "com.android.tools.build:gradle:$androidGradlePluginVersion"
    classpath "com.episode6.hackit.deployable:deployable:$deployableVersion"
    classpath "org.jetbrains.dokka:dokka-android-gradle-plugin:$dokkaVersion"
  }
}

apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'
apply plugin: 'com.episode6.hackit.deployable.kt.aar'
```

#### Groovy Setup
If this is a groovy project you'll want to pair the deployable.jar plugin with the groovydocs addon
```groovy
apply plugin: 'com.episode6.hackit.deployable.jar'
apply plugin: 'com.episode6.hackit.deployable.addon.groovydocs'
```

#### gradle.properties setup
Add the common pom elements to your root `gradle.properties`
```
deployable.pom.description=Gradle plugins to ease the pain of creating deployable jars and aars
deployable.pom.url=https://github.com/episode6/deployable
deployable.pom.scm.url=extensible
deployable.pom.scm.connection=scm:https://github.com/episode6/deployable.git
deployable.pom.scm.developerConnection=scm:https://github.com/episode6/deployable.git
deployable.pom.license.name=The MIT License (MIT)
deployable.pom.license.url=https://github.com/episode6/deployable/blob/master/LICENSE
deployable.pom.license.distribution=repo
deployable.pom.developer.id=episode6
deployable.pom.developer.name=episode6, Inc.

deployable.nexus.releaseRepoUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2/
deployable.nexus.snapshotRepoUrl=https://oss.sonatype.org/content/repositories/snapshots/
```

In your system's `~/.gradle/gradle.properties` add your nexus login info and signing key info
```
deployable.nexus.username=<username>
deployable.nexus.password=<password>

signing.keyId=<keyId>
signing.password=<keyPassword>
signing.secretKeyRingFile=<pathToKeyringFile>
```
**WARNING**: DON'T PUT PASSWORDS IN YOUR REPO! The above file belongs in your home directory at `~/.gradle/gradle.properties`

#### build.gradle setup
Most of deployable's properties can alternatively be set or overridden directly in your `build.gradle`
```groovy
apply plugin: 'java-library'
apply plugin: 'com.episode6.hackit.deployable.jar'

deployable {
    pom {
        description "ProjectDescription"
        url "http://projectUrl"
        scm {
            url "extensible"
            connection "scm:http://connection"
            developerConnection "scm:http://developerConnection"
        }
        license {
            name "License Name"
            url "http://licenseUrl"
            distrobution "repo"
        }
        developer {
            id "developerId"
            name "developerName"
        }
    }
    nexus {
        username "username"
        password "password"
        snapshotRepoUrl "https://shapshotRepo"
        releaseRepoURl "https://releaseRepo"
    }
}
```

#### Deploy
Finally, deploy using
`./gradlew publish` or the new deploy alias `./gradlew deploy`

#### Default Dependency Mapping
Deployable includes built-in mapping for dependencies declared in `api` and `implementation` configurations (into the dependency section of the maven pom output). We also add `mavenOptional`, `mavenProvided` and `mavenProvidedOptional` configurations which will also be mapped automatically.
```groovy
dependencies {
    // api -> maven: 'compile'
    api 'com.example:example-core:1.0'

    // implementation -> maven: 'runtime'
    implementation 'com.example:example-addition:1.0'

    // implementation -> maven: 'runtime' + 'optional=true'
    mavenOptional 'com.example:example-addition:1.0'

    // mavenProvided -> maven: 'provided'
    mavenProvided 'com.example:example-dep:1.0'

    // mavenProvidedOptional -> maven: 'provided' + 'optional=true'
    mavenProvidedOptional 'com.otherexample:other-dep:1.0', optional
}
```

#### Customize Dependencies
To map dependencies of other configurations to the maven pom use the `deployable.pom.dependencyConfigurations` block...
```groovy
configurations {
    someCompileConfig
    someProvidedConfig
    someCompileOptionalConfig
    someProvidedOptionalConfig
}

deployable {
    pom {
        dependencyConfigurations {

            // clear all built-in mappings
            clear()

            // remove a specific gradle configuration from being mapped to the maven pom
            unmap "implementation"

            // map configurations either by reference or just its name (if it might not exist)
            map configurations.someCompileConfig, "compile"
            map "someProvidedConfig", "provided"

            // map optional configs using mapOptional
            mapOptional configurations.someCompileOptionalConfig, "compile"
            mapOptional "someProvidedOptionalConfig", "provided"
        }
    }
}

dependencies {
    someCompileConfig 'com.example:compile-dep:1.0'
    someProvidedConfig 'com.example:provided-dep:1.0'
    someCompileOptionalConfig 'com.example:compile-optional-dep:1.0'
    someProvidedOptionalConfig 'com.example:provided-optional-dep:1.0'
}
```

#### Excluding Sources and Docs
By default, deployable configures and includes source and javadocs in generated publications. You can override this behavior either via `gradle.properties` or `build.gradle`
```
# gradle.properties

deployable.publication.includeSources=false
deployable.publication.includeDocs=false
```

```groovy
// build.gradle

deployable {
    publication {
        includeSources false
        includeDocs false
    }
}
```

#### Customize Published Artifacts
To modify or amend the actual publication or included artifacts (i.e. what jars will actually be signed and published), use the `deployable.publication` block's main and amend methods to configure the MavenPublication lazily.
```groovy
deployable {
    publication {

        // replace the main artifact published by this project
        main {
            artifact altJarTask
        }

        // amend a sources artifact that won't be applied if includeSources == false
        amendSources {
            artifact altSourcesTask
        }

        // amend a docs artifact that won't be applied if includeDocs == false
        amendDocs {
            artifact altDocsJarTask
        }

        // amend artifacts or configuration to the publication that will always be applied
        amend {
            artifact extraDocsTask
        }
    }
}
```

#### Customize POM as XML
If you need to make some additional changes to the pom xml output at the last-mile, use the `deployable.pom.withXml {}` block
```groovy
deployable {
    pom {
        withXml {
            appendNode("someNewNode")
        }
    }
}
```

### Why does it exist?
This was my first gradle plugin and groovy project so it is still rough around a few edges. The main goal here was to abstract away as much of the boilerplate of publishing a maven-deployable library as possible, and make it quick and painless to create and deploy new open-source libraries. Having an abstraction-layer on top of 3rd party tools also grants the flexibility to adapt and should enable future support for more types of repos without requiring changes to individual project configuration.

### License
MIT: https://github.com/episode6/deployable/blob/master/LICENSE
