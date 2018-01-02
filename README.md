tools.gitlibs
========================================

An API for retrieving, caching, and programatically accessing git libraries.

## Rationale

To access git dependencies (for example, via tools.deps), we need some way to download git libraries
and cache the working tree as indicated by git shas. The git library can be maintained and updated
via fetch. The working trees (at a sha) are immutable and can be cached and reused by anyone.

## Authentication

## Release Information

Latest release: none yet

* [All released versions](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.gitlibs%22)
* Coordinates:  `org.clojure/tools.gitlibs "x.y.z"`

# Developer Information

* [GitHub project](https://github.com/clojure/tools.gitlibs)
* [How to contribute](https://dev.clojure.org/display/community/Contributing)
* [Bug Tracker](https://dev.clojure.org/jira/browse/TDEPS)
* [Continuous Integration](https://build.clojure.org/job/gitlibs/)
* [Compatibility Test Matrix](https://build.clojure.org/job/tools.gitlibs-matrix/)

# Copyright and License

Copyright © 2018 Rich Hickey, Alex Miller, and contributors

All rights reserved. The use and
distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file
epl-v10.html at the root of this distribution. By using this software
in any fashion, you are agreeing to be bound by the terms of this
license. You must not remove this notice, or any other, from this
software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
