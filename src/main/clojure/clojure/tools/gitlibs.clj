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

(defn cache-dir
  "Return the root gitlibs cache directory. By default ~/.gitlibs or
  override by setting the environment variable GITLIBS."
  []
  (impl/cache-dir))

(defn resolve
  "Takes a git url and a rev, and returns the full commit sha. rev may be a
  partial sha, full sha, or tag name."
  [url rev]
  (impl/git-rev-parse (impl/ensure-git-dir url) rev))

(defn procure
  "Procure a working tree at rev for the git url representing the library lib,
  returns the directory path. lib is a qualified symbol where the qualifier is a
  controlled or conveyed identity, or nil if rev is unknown."
  [url lib rev]
  (let [lib-dir (jio/file (impl/cache-dir) "libs" (namespace lib) (name lib))
        sha (or (impl/match-exact lib-dir rev) (impl/match-prefix lib-dir rev) (resolve url rev))]
    (when sha
      (let [sha-dir (jio/file lib-dir sha)]
        (when-not (.exists sha-dir)
          (impl/printerrln "Checking out:" url "at" rev)
          (impl/git-checkout url sha-dir sha))
        (.getCanonicalPath sha-dir)))))

(defn descendant
  "Returns rev in git url which is a descendant of all other revs,
  or nil if no such relationship can be established."
  [url revs]
  (when (seq revs)
    (let [git-dir (impl/ensure-git-dir url)
          shas (map (partial impl/git-rev-parse git-dir) revs)]
      (if (not-empty (filter nil? shas))
        nil ;; can't resolve all shas in this repo
        (first (sort (partial impl/commit-comparator git-dir) shas))))))
