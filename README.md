tools.gitlibs
========================================

An API for retrieving, caching, and programatically accessing git libraries.

## Rationale

To access git dependencies (for example, via tools.deps), one must download git directories
and working trees as indicated by git shas. This library provides this functionality and also
builds a cache of git dirs and working trees that can be reused.

## Usage

```clojure
(require '[clojure.tools.gitlibs :as gl])

;; Obtain a "git repo" - this will ensure the git directory is downloaded
;; and cached and will return a package of information that can be used
;; in subsequent calls
(def git-repo (gl/ensure-git-dir "https://github.com/clojure/spec.alpha.git"))

;; git-repo:
;; {:git-dir #object[java.io.File 0x67817f98 "/Users/me/.gitlibs/_repos/github.com/clojure/spec.alpha"],
;;  :url "https://github.com/clojure/spec.alpha.git"}

;; Obtain the working tree at a particular (partial) sha - may also use
;; full shas or annotated tag names
(def wt (gl/ensure-working-tree git-repo 'org.clojure/spec.alpha "739c1af5"))

;; wt:
;; #object[java.io.File 0x4641b1fe "/Users/alex/.gitlibs/libs/org.clojure/spec.alpha/739c1af5"]

;; Resolve a partial sha to a full sha:
(def commit (gl/full-sha git-repo "739c1af5"))

;; commit:
;; "739c1af56dae621aedf1bb282025a0d676eff713"

;; Check whether some dummy commit is an ancestor
(gl/ancestor? git-repo "12345678901234567890abcdefabcdefabcdefab" commit)
false
```

## Authentication

The following git url types are supported:

* https - for public anonymous clone and fetch of public repos
* ssh - for authenticated clone and fetch of private repos
* http - may work for public repos, but not supported or recommended
* git - may work for public repos, but not supported or recommended

ssh authentication works by connecting to the local ssh agent, which must have a registered identity. To check whether you have registered identities, use:

`ssh-add -l`

which should return one or more registered identities, usually one at ~/.ssh/id_rsa. If you don't see one but you have an ssh key, you can add it with:

`ssh-add ~/.ssh/id_rsa`

If your key has a passphrase, you will need to enter it at this time.

## Cache directory

By default, the cache will be built in ~/.gitlibs/. To change the cache location, use kwarg option
`:cache-dir "/tmp/somewhere"` on `ensure-git-dir` and `ensure-working-tree`.

## Release Information

Latest release: 0.1.8 

* [All released versions](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.gitlibs%22)
* Coordinates:  `org.clojure/tools.gitlibs {:mvn/version "0.1.8"}`

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
