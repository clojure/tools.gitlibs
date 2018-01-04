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
    [org.eclipse.jgit.revwalk RevWalk RevCommit]
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
        rev-dir (jio/file impl/cache-dir "libs" (namespace lib) (name lib) full-sha)]
    (when (not (.exists rev-dir))
      (impl/printerrln "Checking out:" url "at" rev)
      (impl/git-checkout url rev-dir full-sha))
    (.getCanonicalPath rev-dir)))

(defn- commit-comparator
  [^RevWalk walk ^RevCommit x ^RevCommit y]
  (cond
    (= x y) 0
    (.isMergedInto walk x y) -1
    (.isMergedInto walk y x) 1
    :else (throw (ex-info "" {}))))

(defn descendant
  "Returns rev in git url which is a descendant of all other revs,
  or nil if no such relationship can be established."
  [url revs]
  (let [walk (RevWalk. (-> url impl/ensure-git-dir impl/git-repo))]
    (try
      (let [shas (map (partial resolve url) revs)]
        (if (not-empty (filter nil? shas))
          nil ;; can't resolve all shas in this repo
          (let [commits (map #(.lookupCommit walk (ObjectId/fromString ^String %)) shas)
                ^RevCommit ret (first (sort (partial commit-comparator walk) commits))]
            (.. ret getId name))))
      (catch MissingObjectException e nil)
      (catch clojure.lang.ExceptionInfo e nil)
      (finally (.dispose walk)))))

(comment
  (resolve "https://github.com/clojure/spec.alpha.git" "739c1af5")
  (procure "https://github.com/clojure/spec.alpha.git" 'org.clojure/spec.alpha "739c1af5")
  (descendant "https://github.com/clojure/spec.alpha.git" ["607aef0" "739c1af"]) ;; "607aef0..."
  (descendant "https://github.com/clojure/spec.alpha.git" ["739c1af" "607aef0"]) ;; "607aef0..."
  (descendant "https://github.com/clojure/spec.alpha.git" ["1234567" "739c1af"]) ;; nil
  )