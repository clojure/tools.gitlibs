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
    [clojure.tools.gitlibs.impl :as impl])
  (:import
    [java.io File]
    [org.eclipse.jgit.lib ObjectId]
    [org.eclipse.jgit.revwalk RevWalk]
    [org.eclipse.jgit.errors MissingObjectException]))

(defn resolve
  "Takes a git url and a rev, and returns the full commit sha. rev may be a
  partial sha, full sha, or tag name."
  [url rev]
  (let [git-dir (impl/ensure-git-dir url)]
    (if (ObjectId/isId rev)
      rev
      (let [rev (.resolve (impl/git-repo git-dir) rev)]
        (if rev
          (.getName rev)
          nil)))))

(defn procure
  "Procure a working tree at rev for the git url representing the library lib,
  returns the directory path. lib is a qualified symbol where the qualifier is a
  controlled or conveyed identity."
  [url lib rev]
  (let [git-dir (jio/file (impl/ensure-git-dir url))
        full-sha (resolve url rev)
        rev-dir (jio/file @impl/default-git-dir "libs" (namespace lib) (name lib) full-sha)]
    (when (not (.exists rev-dir))
      (impl/git-checkout (impl/git-fetch git-dir rev-dir) full-sha))
    (.getCanonicalPath rev-dir)))

(defn ancestor?
  "Checks whether ancestor-rev is an ancestor of child-rev."
  [child-url ^String ancestor-rev ^String child-rev]
  (let [child-git-dir (impl/ensure-git-dir child-url)
        repo (impl/git-repo child-git-dir)
        walk (RevWalk. repo)]
    (try
      (let [child-sha (resolve child-url child-rev)
            ancestor-sha (resolve child-url ancestor-rev)]
        (if (and child-sha ancestor-sha)
          (let [child-commit (.lookupCommit walk (ObjectId/fromString child-sha))
                ancestor-commit (.lookupCommit walk (ObjectId/fromString ancestor-sha))]
            (.isMergedInto walk ancestor-commit child-commit))
          ;; throw?
          false))
      (catch MissingObjectException e false)
      (finally (.dispose walk)))))

(comment
  (resolve "https://github.com/clojure/spec.alpha.git" "739c1af5")
  (procure "https://github.com/clojure/spec.alpha.git" 'org.clojure/spec.alpha "739c1af5")
  (ancestor? "https://github.com/clojure/spec.alpha.git" "607aef0" "739c1af5") ;; true
  (ancestor? "https://github.com/clojure/spec.alpha.git" "739c1af5" "607aef0") ;; false
  (ancestor? "https://github.com/clojure/spec.alpha.git" "1234567" "739c1af5") ;; false
  )