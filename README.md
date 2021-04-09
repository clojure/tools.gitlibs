tools.gitlibs
========================================

An API for retrieving, caching, and programatically accessing git libraries.

## Rationale

To access git dependencies (for example, via tools.deps), one must download git directories
and working trees as indicated by git shas. This library provides this functionality and also
keeps a cache of git dirs and working trees that can be reused.

## Usage

The following API is provided in `clojure.tools.gitlibs`:

* `(resolve git-url rev) ;; returns full sha of rev in git-url`
* `(procure git-url lib rev) ;; returns working tree directory for git-url identified as lib at rev`
* `(descendant git-url revs) ;; returns rev which is a descedant of all revs, or nil if none`

### Git urls

The following git url types are supported:

* `https` - for public anonymous clone and fetch of public repos
* `ssh` - for authenticated clone and fetch of private repos
* `http` and `git` protocols are plain-text and not supported or recommended

### SSH authentication for private repositories

ssh authentication works by connecting to the local ssh agent (ssh-agent on \*nix or Pageant via PuTTY on Windows).
The ssh-agent must have a registered identity for the key being used to access the Git repository.
To check whether you have registered identities, use:

`ssh-add -l`

which should return one or more registered identities, typically the one at `~/.ssh/id_rsa`.

For more information on creating keys and using the ssh-agent to manage your ssh identities, GitHub provides excellent info:

* https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/
* https://help.github.com/articles/working-with-ssh-key-passphrases/

### Revs

The API functions all take revs, which can be any git rev that resolves to a commit, such as:

* Full sha (40 chars)
* Prefix sha (sufficiently unique in the repo)
* Tag name
* Branch name

Procured working trees are always cached on the basis of the rev's full sha, so using `procure` 
repeatedly on a rev that does not resolve to a fixed sha may result in new checkouts in the cache.

### Configuration

Downloaded git dirs and working trees are stored in ~/.gitlibs - this directory is just a cache and can be safely removed if needed.

tools.gitlibs can be configured by either environment variable or Java system property. If both are provided, the Java system property takes precedence.

| Env var | Java system property | default | description
| ------- | -------------------- | ------- | -----------
| GITLIBS | clojure.gitlibs.dir | ~/.gitlibs | Local directory cache for git repos and working trees |
| GITLIBS_COMMAND | clojure.gitlibs.command | git | git command to run when shelling out (supply full path if needed) |
| GITLIBS_DEBUG | clojure.gitlibs.debug | false | If true, print git commands and output to stderr |
| GITLIBS_TERMINAL | clojure.gitlibs.terminal | false | If true, interactively prompt if needed |

## Example Usage

```clojure
(require '[clojure.tools.gitlibs :as gl])

;; Given a git repo url and a rev, resolve to a full sha.
(gl/resolve "https://github.com/clojure/spec.alpha.git" "739c1af")
;; => "739c1af56dae621aedf1bb282025a0d676eff713"

;; Given a git repo url, library identifier, and a rev, return a path to the working tree
(gl/procure "https://github.com/clojure/spec.alpha.git" 'org.clojure/spec.alpha "739c1af")
;; => "/Users/me/.gitlibs/libs/org.clojure/spec.alpha/739c1af56dae621aedf1bb282025a0d676eff713"

;; Given git repo url, and a collection of revs, return the full sha of the one commit that is
;; a descendant of all other revs or nil if no such rev exists in the collection.
(gl/descendant "https://github.com/clojure/spec.alpha.git" ["607aef0" "739c1af"])
;; => "739c1af56dae621aedf1bb282025a0d676eff713"
```

## Release Information

This project follows the version scheme MAJOR.MINOR.COMMITS where MAJOR and MINOR provide some relative indication of the size of the change, but do not follow semantic versioning. In general, all changes endeavor to be non-breaking (by moving to new names rather than by breaking existing names). COMMITS is an ever-increasing counter of commits since the beginning of this repository.

Latest release: 2.3.161

* [All released versions](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.gitlibs%22)
* Coordinates: `org.clojure/tools.gitlibs {:mvn/version "2.3.161"}`

# Developer Information

* [GitHub project](https://github.com/clojure/tools.gitlibs)
* [API Docs](https://clojure.github.io/tools.gitlibs)
* [How to contribute](https://dev.clojure.org/display/community/Contributing)
* [Bug Tracker](https://dev.clojure.org/jira/browse/TDEPS)
* [Continuous Integration](https://build.clojure.org/job/gitlibs/)
* [Compatibility Test Matrix](https://build.clojure.org/job/tools.gitlibs-matrix/)

# Copyright and License

Copyright © 2018-2021 Rich Hickey, Alex Miller, and contributors

All rights reserved. The use and
distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file
epl-v10.html at the root of this distribution. By using this software
in any fashion, you are agreeing to be bound by the terms of this
license. You must not remove this notice, or any other, from this
software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
