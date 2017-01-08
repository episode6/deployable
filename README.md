Deployable
==========
Some quick and dirty gradle plugins to ease the pain of deploying jars and aars to sonatype. This currently uses the old `maven` plugin instead of the new `maven-publish` plugin.

## Usage
Add Deployable to the classpath in your root `build.gradle`
```groovy
buildscript {
  dependencies {
    classpath 'com.episode6.hackit.deployable:deployable:0.0.0.1-SNAPSHOT'
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
POM_DESCRIPTION=Gradle plugins to ease the pain of creating deployable jars and aars
POM_URL=https://github.com/episode6/deployable
POM_SCM_URL=extensible
POM_SCM_CONNECTION=scm:https://github.com/episode6/deployable.git
POM_SCM_DEV_CONNECTION=scm:https://github.com/episode6/deployable.git
POM_LICENCE_NAME=The MIT License (MIT)
POM_LICENCE_URL=https://github.com/episode6/deployable/blob/master/LICENSE
POM_LICENCE_DIST=repo
POM_DEVELOPER_ID=episode6
POM_DEVELOPER_NAME=episode6, Inc.
```

In your computer's `~/.gradle/gradle.properties` add your nexus login info and signing key info
```
NEXUS_USERNAME=<username>
NEXUS_PASSWORD=<password>

signing.keyId=<keyId>
signing.password=<keyPassword>
signing.secretKeyRingFile=<pathToKeyringFile>
```

You can optionally override the repository urls by adding the following to your `gradle.properties`
```
NEXUS_RELEASE_REPOSITORY_URL=https://oss.sonatype.org/service/local/staging/deploy/maven2/
NEXUS_SNAPSHOT_REPOSITORY_URL=https://oss.sonatype.org/content/repositories/snapshots/
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
apply plugin: 'com.episode6.hackit.deployable.addons.groovydocs'
```


Finally, deploy using the uploadArchives task

### License
MIT: https://github.com/episode6/chop/blob/master/LICENSE