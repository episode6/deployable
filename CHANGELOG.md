# Deployable ChangeLog

### v0.2.0-SNAPSHOT - unreleased
- Re-write plugin to use `maven-publish` instead of old `maven` plugin
    - Api stays mostly the same, deploy and install tasks are still valid
- Customizing deployable artifacts has changed, we now use the `deployable.publication { artifact task }`
- Add config block to customize pom as xml (after deployable has done its initial setup) `deployable.withPomXml { }`


### v0.1.12 - released 5/28/2018
- introduce kotlin support
- start changelog
