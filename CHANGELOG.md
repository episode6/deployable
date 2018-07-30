# Deployable ChangeLog

### v0.2.1-SNAPSHOT - unreleased
- Fix bug in `gradle-plugin` plugin

### v0.2.0-SNAPSHOT - released July 29, 2018
- Re-write plugin to use `maven-publish` instead of old `maven` plugin
- **[BREAKING]** Customizing deployable artifacts (formerly via maven's `archives {}` block has changed, we now use the `deployable.publication.main { artifact mainOutputTask }` and `deployable.publication.amend { artifact additionalTask }` blocks. See [README for more info](README.md#customize-published-artifacts)
- **[BREAKING]** Stop providing default values for repo urls. If no urls are specified, no repo will be set up.
- **[BREAKING]** Moved `mavenDependencies {}` block to `deployable.pom.dependencyConfigurations {}`. Api remains mostly the same, but scope priorities are no longer used. We also added a `clear()` method. See [README for more info](README.md#customize-dependencies)
- Add config block to customize pom as xml (after deployable has done its initial setup) `deployable.pom.withXml { }`. See [README for more info](README.md#customize-pom-as-xml)
- Add new plugin `com.episode6.hackit.deployable.gradle-plugin` to workaround java-gradle-plugins build in publish config


### v0.1.12 - released May 28th, 2018
- introduce kotlin support
- start changelog
