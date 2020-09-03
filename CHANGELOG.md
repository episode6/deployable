# Deployable ChangeLog

### v0.2.5 - Unreleased


### v0.2.3 - release March 6th, 2019
- Upgrade to gradle 5.1.1
- Now tested with/supporting android gradle plugin 3.3.0

### v0.2.2 - released August 7th, 2018
- Add publication params for `includeSources` and `includeDocs`. If unset, default is true, so there should be no need to update existing projects. See [README for more info](README.md#excluding-sources-and-docs)
- Add plugin publication configuration methods for `amendSources` and `amendDocs`. These configuration steps will be skipped if `includeSources` / `includeDocs` are false (respectively). See [README for more info](README.md#customize-published-artifacts)
- Add experimental [bintray plugin](buildSrc/src/main/groovy/com/episode6/hackit/deployable/addon/BintrayAddonPlugin.groovy)
- Add (optional) support for `deployable.pom.developer.email`

### v0.2.1 - released July 29, 2018
- Fix bug in `gradle-plugin` plugin

### v0.2.0-SNAPSHOT - released July 29, 2018
- Re-write plugin to use `maven-publish` instead of old `maven` plugin
- **[BREAKING]** Customizing deployable artifacts (formerly via maven's `archives {}` block) has changed, we now use the `deployable.publication.main { artifact mainOutputTask }` and `deployable.publication.amend { artifact additionalTask }` blocks. See [README for more info](README.md#customize-published-artifacts)
- **[BREAKING]** Stop providing default values for repo urls. If no urls are specified, no repo will be set up.
- **[BREAKING]** Moved `mavenDependencies {}` block to `deployable.pom.dependencyConfigurations {}`. Api remains mostly the same, but scope priorities are no longer used. We also added a `clear()` method. See [README for more info](README.md#customize-dependencies)
- Add config block to customize pom as xml (after deployable has done its initial setup) `deployable.pom.withXml { }`. See [README for more info](README.md#customize-pom-as-xml)
- Add new plugin `com.episode6.hackit.deployable.gradle-plugin` to workaround java-gradle-plugins build in publish config


### v0.1.12 - released May 28th, 2018
- introduce kotlin support
- start changelog
