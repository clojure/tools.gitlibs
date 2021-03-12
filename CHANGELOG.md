Changelog
===========

* next
  * Fix rev-parse check on unfetched sha to report failure
* 2.1.139 on Mar 11, 2021
  * Add tags method
* 2.1.129 on Mar 10, 2021
  * Change git options during checkout to work with more git versions
  * Check exit code on checkout and throw if failed
  * Overhaul config and debug settings
  * Change min Clojure dep to 1.9
* 2.0.119 on Mar 10, 2021
  * Fix issues with checkouts of multiple commits per repo
  * resolve now only fetches if it can't resolve a ref
  * Config read from most to least preferred: Java system property / env var / default:
    * gitlibs dir: `clojure.gitlibs.dir` / `GITLIBS` / `nil`
    * git command: `clojure.gitlibs.command` / `GITLIBS_COMMAND` / `"git"`
  * Fix reflection error
* 2.0.109 on Mar 3, 2021
  * TDEPS-91 Replace jgit implementation by shelling out to git
* 1.0.100 on Aug 20, 2020
  * Fetch all tags on procure
* 1.0.96 on Aug 7, 2020
  * TDEPS-160 - Add missing compression lib for ssh compression
* 1.0.91 on June 9, 2020
  * Fix reflection warning
* 1.0.83 on Feb 18, 2020
* 0.2.64 on Mar 16, 2018
  * TDEPS-53 If cached gitlib dir is corrupt, re-clone
* 0.2.59 on Feb 9, 2018
  * Add `cache-dir` API function
* 0.2.54 on Jan 10, 2018
  * Make gitlibs cache dir configurable via environment variable GITLIBS
* 0.2.47 on Jan 8, 2018 
  * Bump jgit to latest release
  * In procure, use prefix to find working tree before needing to clone
* 0.2.40 on Jan 4, 2018
  * Add tests
  * Fix descendant comparator order
* 0.2.32 on Jan 3, 2018
  * Clean up initial API
  * Add clone / checkout print to console
  * Fix bug with fetching new commits on existing repos
* 0.1.8 on Jan 2, 2018
  * Initial release
