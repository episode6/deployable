Deployable for Gradle
=====================
Gradle plugins to ease the pain of deploying jars and aars to maven repositories. This currently uses the old `maven` plugin instead of the new `maven-publish` plugin.

## Usage
Add Deployable to the classpath in your root `build.gradle`
```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.episode6.hackit.deployable:deployable:0.1.2'
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
```

In your computer's `~/.gradle/gradle.properties` add your nexus login info and signing key info
```
deployable.nexus.username=<username>
deployable.nexus.password=<password>

signing.keyId=<keyId>
signing.password=<keyPassword>
signing.secretKeyRingFile=<pathToKeyringFile>
```

You can optionally override the repository urls by adding the following to your `gradle.properties`
```
deployable.nexus.releaseRepoUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2/
deployable.nexus.snapshotRepoUrl=https://oss.sonatype.org/content/repositories/snapshots/
```

In each deployable sub-module apply the plugin to `build.gradle`
```groovy
// to deploy a JAR
apply plugin: 'com.episode6.hackit.deployable.jar'

// to deploy an AAR
apply plugin: 'com.episode6.hackit.deployable.aar'
```

If this is a groovy project you'll want to pair the deployable.jar plugin with the groovydocs addon
```groovy
apply plugin: 'com.episode6.hackit.deployable.jar'
apply plugin: 'com.episode6.hackit.deployable.addon.groovydocs'
```

Most of deployable's properties can alternatively be set or overridden directly in your `build.gradle`
```groovy
apply plugin: 'java'
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
`./gradlew uploadArchives` or the new deploy alias `./gradlew deploy`

### Why does it exist?
This is my first gradle plugin and groovy project so it may be rough around the edges. There are probably better tools out there for your open source libraries, but this will be building block for upcoming episode6 open source projects.

### License
MIT: https://github.com/episode6/deployable/blob/master/LICENSE