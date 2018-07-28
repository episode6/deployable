Deployable for Gradle
=====================
Gradle plugins to ease the pain of deploying jars and aars to maven repositories.

## Usage
Add Deployable to the classpath in your root `build.gradle`
```groovy
buildscript {
  repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
  }
  dependencies {
    classpath 'com.episode6.hackit.deployable:deployable:0.2.0-SNAPSHOT'
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

// to deploy an AAR
apply plugin: 'com.episode6.hackit.deployable.aar'
```

If you need kotlin support, you must also include dokka on your buildscript classpath (for javadoc support) and use one of the `kt` plugins instead
```groovy
// deployable JAR with Kotlin support

buildscript {
  repositories { jcenter() }
  dependencies {
    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

    // deployable's kt plugin applies the dokka plugin, so it must be
    // included on the buildscript classpath
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
  repositories { jcenter() }
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

If this is a groovy project you'll want to pair the deployable.jar plugin with the groovydocs addon
```groovy
apply plugin: 'com.episode6.hackit.deployable.jar'
apply plugin: 'com.episode6.hackit.deployable.addon.groovydocs'
```

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

Finally, deploy using
`./gradlew publish` or the new deploy alias `./gradlew deploy`

Deployable also adds some basic support for maven's `provided` scope and `optional` flag via custom scopes...
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

To map dependencies of extra configurations to the maven pom use the `deployable.pom.dependencyConfigurations` block...
```groovy
configurations {
    someCompileConfig
    someProvidedConfig
    someCompileOptionalConfig
    someProvidedOptionalConfig
}

deployable.pom.dependencyConfigurations {
    // map with configuration reference
    map configurations.someCompileConfig, "compile"

    // map with configuration name (and ignore if it doesnt exist)
    map "someProvidedConfig", "provided"

    // map optional configs using mapOptional
    mapOptional configurations.someCompileOptionalConfig, "compile"
    mapOptional "someProvidedOptionalConfig", "provided"

    // remove the mapping of a gradle configuration
    unamp configurations.api
    unmap "implementation"
}

dependencies {
    someCompileConfig 'com.example:compile-dep:1.0'
    someProvidedConfig 'com.example:provided-dep:1.0'
    someCompileOptionalConfig 'com.example:compile-optional-dep:1.0'
    someProvidedOptionalConfig 'com.example:provided-optional-dep:1.0'
}
```

### Why does it exist?
This is my first gradle plugin and groovy project so it may be rough around the edges. There are probably better tools out there for your open source libraries, but this will be building block for upcoming episode6 open source projects.

### License
MIT: https://github.com/episode6/deployable/blob/master/LICENSE