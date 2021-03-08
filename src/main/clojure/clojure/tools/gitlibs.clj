;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.gitlibs
  "An API for retrieving and caching git repos and working trees.

  The git url can be either an https url for anonymous checkout or an ssh url
  for private access. revs can be either full sha, prefix sha, or tag name."
  (:refer-clojure :exclude [resolve])
  (:require
    [clojure.java.io :as jio]
    [clojure.tools.gitlibs.impl :as impl]))

(set! *warn-on-reflection* true)

(defn cache-dir
  "Return the root gitlibs cache directory. By default ~/.gitlibs or
  override by setting the environment variable GITLIBS."
  []
  (impl/cache-dir))

;; Possible new API, internal for now
(defn- resolve-all
  "Takes a git url and a coll of revs, and returns the full commit shas.
  Each rev may be a partial sha, full sha, or tag name. Returns nil for
  unresolveable revs.

  Optional opts map may include:
    :interactive (default false) - set true to allow stdin prompts (example: unknown host)
    :print-commands (default false) - set true to write git executions to stderr"
  ([url revs]
   (resolve-all url revs nil))
  ([url revs opts]
   (let [git-dir (impl/ensure-git-dir url opts)]
     (reduce
       (fn [rs r]
         (if-let [res (impl/git-rev-parse git-dir r opts)]
           (conj rs res)
           (do ;; could not resolve - fetch and try again
             (impl/git-fetch (jio/file git-dir) opts)
             (conj rs (impl/git-rev-parse git-dir r opts)))))
       [] revs))))

(defn resolve
  "Takes a git url and a rev, and returns the full commit sha or nil if can't
  resolve. rev may be a partial sha, full sha, or tag name.

  Optional opts map may include:
    :interactive (default false) - set true to allow stdin prompts (example: unknown host)
    :print-commands (default false) - set true to write git executions to stderr"
  ([url rev]
   (resolve url rev nil))
  ([url rev opts]
   (first (resolve-all url [rev] opts))))

(defn procure
  "Procure a working tree at rev for the git url representing the library lib,
  returns the directory path. lib is a qualified symbol where the qualifier is a
  controlled or conveyed identity, or nil if rev is unknown.

  Optional opts map may include:
    :interactive (default false) - set true to allow stdin prompts (example: unknown host)
    :print-commands (default false) - set true to write git commands to stderr"
  ([url lib rev]
   (procure url lib rev nil))
  ([url lib rev opts]
   (let [lib-dir (impl/lib-dir lib)
         sha (or (impl/match-exact lib-dir rev) (impl/match-prefix lib-dir rev) (resolve url rev))]
     (when sha
       (let [sha-dir (jio/file lib-dir sha)]
         (when-not (.exists sha-dir)
           (impl/printerrln "Checking out:" url "at" rev)
           (impl/git-checkout url sha-dir sha opts))
         (.getCanonicalPath sha-dir))))))

(defn descendant
  "Returns rev in git url which is a descendant of all other revs,
  or nil if no such relationship can be established.

  Optional opts map may include:
    :interactive (default false) - set true to allow stdin prompts (example: unknown host)
    :print-commands (default false) - set true to write git commands to stderr"
  ([url rev]
   (descendant url rev nil))
  ([url revs opts]
   (when (seq revs)
     (let [shas (resolve-all url revs opts)]
       (if (seq (filter nil? shas))
         nil ;; can't resolve all shas in this repo
         (let [git-dir (impl/ensure-git-dir url opts)]
           (->> shas (sort (partial impl/commit-comparator git-dir opts)) first)))))))

(comment
  (resolve "git@github.com:clojure/tools.gitlibs.git" "11fc774" {:print-commands true :interactive true})
  (descendant "https://github.com/clojure/tools.gitlibs.git" ["5e2797a487c" "11fc774" "d82adc29" "815e312310"] {:print-commands true})

  (println
    @(future (procure "https://github.com/clojure/tools.gitlibs.git" 'org.clojure/tools.gitlibs "11fc77496f013871c8af3514bbba03de0af28061"))
    @(future (procure "https://github.com/clojure/tools.gitlibs.git" 'org.clojure/tools.gitlibs "11fc77496f013871c8af3514bbba03de0af28061")))

  )