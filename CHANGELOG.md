# Deployable ChangeLog

### v0.2.0-SNAPSHOT - unreleased
- Re-write plugin to use `maven-publish` instead of old `maven` plugin
    - Api stays mostly the same, deploy and install tasks are still valid
- **[breaking]** Customizing deployable artifacts has changed, we now use the `deployable.publication { artifact task }`
- Add config block to customize pom as xml (after deployable has done its initial setup) `deployable.withPomXml { }`
- Add new plugin `com.episode6.hackit.deployable.gradle-plugin` to workaround java-gradle-plugins build in publish config
- **[breaking]** Stop providing default values for repo urls. If no urls are specified, no repo will be set up.


### v0.1.12 - released 5/28/2018
- introduce kotlin support
- start changelog
