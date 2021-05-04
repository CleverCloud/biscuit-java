# biscuit-java

[![Central Version](https://img.shields.io/maven-central/v/com.clever-cloud/biscuit-java)](https://mvnrepository.com/artifact/com.clever-cloud/biscuit-java)
[![Nexus Version](https://img.shields.io/nexus/r/com.clever-cloud/biscuit-java?server=https%3A%2F%2Foss.sonatype.org)](https://search.maven.org/artifact/com.clever-cloud/biscuit-java)

[Biscuit's](https://github.com/CleverCloud/biscuit) Java library implementation.

## Usage

```java
<!-- https://mvnrepository.com/artifact/com.clever-cloud/biscuit-java -->
<dependency>
    <groupId>com.clever-cloud</groupId>
    <artifactId>biscuit-java</artifactId>
    <version>@VERSION@</version>
</dependency>
```

## Development

### Requirements

* JDK v11
* the Protobuf compiler command `protoc` v3+ is required in `$PATH`.

### Build

```bash
mvn clean install
# skip tests
mvn clean install -DskipTests
```

## Publish

### Release process

Set the version in `pom.xml`.

Commit and tag the version. Then push and create a GitHub release.

Finally, publishing to Nexus and Maven Central is **automatically triggered by creating a GitHub release** using GitHub Actions.

### GitHub Actions Requirements

Publish requires following secrets:

`OSSRH_USERNAME` the Sonatype username
`OSSRH_TOKEN` the Sonatype token
`OSSRH_GPG_SECRET_KEY` the gpg private key used to sign packages
`OSSRH_GPG_SECRET_KEY_PASSWORD` the gpg private key password

These are stored in GitHub repository's secrets.
