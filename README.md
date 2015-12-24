[![Build Status](https://travis-ci.org/nhl/bootique-cayenne.svg)](https://travis-ci.org/nhl/bootique-cayenne)

Cayenne Integration Bundle for [Bootique](https://github.com/nhl/bootique)

## Note on Cayenne dependency:

_Until official Cayenne 4.0.M3 release is out, you will need to declare an extra repository 
in your pom.xml (unless you have your own repo proxy, in which case add this repo to the proxy) to grab an unofficial build used in this Bootique integration:_

```XML
<repositories>
    <repository>
        <id>cayenne-unofficial-repo</id>
        <name>Cayenne Unofficial Repo</name>
        <url>http://maven.objectstyle.org/nexus/content/repositories/cayenne-unofficial/</url>
    </repository>
</repositories>
```

