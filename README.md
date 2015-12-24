[![Build Status](https://travis-ci.org/nhl/bootique-cayenne.svg)](https://travis-ci.org/nhl/bootique-cayenne)

Cayenne Integration Bundle for [Bootique](https://github.com/nhl/bootique)

## Note on Cayenne dependency:

_This integration is using an unoffical nightly build of Cayenne 4.0.M3 that gives us Java 8 integration and lots of other good things. Until official Cayenne 4.0.M3 release is out, you will need to declare an extra repository in your pom.xml (unless you have your own repo proxy, in which case add this repo to the proxy) to have access to this build:_

```XML
<repositories>
    <repository>
        <id>cayenne-unofficial-repo</id>
        <name>Cayenne Unofficial Repo</name>
        <url>http://maven.objectstyle.org/nexus/content/repositories/cayenne-unofficial/</url>
    </repository>
</repositories>
```

