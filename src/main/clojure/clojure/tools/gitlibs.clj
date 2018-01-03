;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.gitlibs
  (:require
    [clojure.java.io :as jio]
    [clojure.tools.gitlibs.impl :as impl])
  (:import
    [java.io File]
    [org.eclipse.jgit.lib ObjectId]
    [org.eclipse.jgit.revwalk RevWalk]
    [org.eclipse.jgit.errors MissingObjectException]))

(defn full-sha
  "Takes a git url and a rev, and returns the full commit sha. rev may be a
  partial sha, full sha, or annotated tag name."
  [url rev]
  (let [git-dir (impl/ensure-git-dir url)]
    (if (ObjectId/isId rev)
      rev
      (.. (impl/git-repo git-dir) (resolve rev) getName))))

(defn working-tree
  "Ensure a working tree at rev for the git url representing the library lib,
  returns the directory path.

  url should be either an https url for anonymous checkout or an ssh url for
  private access. rev should be a full sha, a sha prefix, or an annotated tag.
  lib is a qualified symbol where the qualifier is a controlled or conveyed identity."
  [url lib rev]
  (let [git-dir (jio/file (impl/ensure-git-dir url))
        rev-dir (jio/file @impl/default-git-dir "libs" (namespace lib) (name lib) rev)]
    (when (not (.exists rev-dir))
      (impl/git-checkout (impl/git-fetch git-dir rev-dir) rev))
    (.getCanonicalPath rev-dir)))

(defn ancestor?
  "Checks whether ancestor-sha is an ancestor of child-sha. Both shas should be full shas."
  [child-git-url ^String ancestor-sha ^String child-sha]
  (let [child-git-dir (impl/ensure-git-dir child-git-url)
        repo (impl/git-repo child-git-dir)
        walk (RevWalk. repo)]
    (try
      (let [child-commit (.lookupCommit walk (ObjectId/fromString child-sha))
            ancestor-commit (.lookupCommit walk (ObjectId/fromString ancestor-sha))]
        (.isMergedInto walk ancestor-commit child-commit))
      (catch MissingObjectException e false)
      (finally (.dispose walk)))))

(comment
  (full-sha "https://github.com/clojure/spec.alpha.git" "739c1af5")
  (working-tree "https://github.com/clojure/spec.alpha.git" 'org.clojure/spec.alpha "739c1af5")
  (ancestor? "https://github.com/clojure/spec.alpha.git" "607aef0643f6cf920293130d45e6160d93fda908" "739c1af56dae621aedf1bb282025a0d676eff713")
  (ancestor? "https://github.com/clojure/spec.alpha.git" "12345678901234567890abcdefabcdefabcdefab" "739c1af56dae621aedf1bb282025a0d676eff713")
  )