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
    [clojure.string :as str]
    [clojure.tools.gitlibs.impl :as impl])
  (:import
    [java.io File]
    [org.eclipse.jgit.lib ObjectId]
    [org.eclipse.jgit.revwalk RevWalk]
    [org.eclipse.jgit.errors MissingObjectException]))

(def ^:private default-git-dir
  (delay
    (let [home (System/getProperty "user.home")
          gitlibs (jio/file home ".gitlibs")]
      (.getAbsolutePath gitlibs))))

(defn- clean-url
  "Chop leading protocol, trailing .git, replace :'s with /"
  [url]
  (-> url
    (str/split #"://")
    last
    (str/replace #"\.git$" "")
    (str/replace #":" "/")))

(defn- ensure-git-dir
  "Ensure the bare git dir for the specified url, returns a map
  representing the git repo. cache-dir will be coerced to a file.

  Optional :cache-dir kwarg can specify root, else ~/.gitlibs."
  [url & {:keys [cache-dir] :or {cache-dir @default-git-dir}}]
  (let [git-dir (jio/file cache-dir "_repos" (clean-url url))]
    (when-not (.exists git-dir)
      (impl/git-clone-bare url git-dir))
    {:git-dir git-dir, :url url}))

(defn ensure-working-tree
  "Ensure a working tree at rev for the git repo (as returned by ensure-git-dir)
  representing the library lib, returns the directory as a java.io.File.

  url should be either an https url for anonymous checkout or an ssh url for
  private access. rev should be a full sha, a sha prefix, or an annotated tag.
  lib is a qualified symbol where the qualifier is a controlled or conveyed identity.

  Optional :cache-dir kwarg can specify root, else ~/.gitlibs."
  ^File [git-repo lib rev & {:keys [cache-dir] :or {cache-dir @default-git-dir}}]
  (let [{:keys [git-dir url]} git-repo
        rev-dir (jio/file cache-dir "libs" (namespace lib) (name lib) rev)]
    (when (not (.exists rev-dir))
      (impl/git-checkout (impl/git-fetch git-dir rev-dir url) rev url))
    rev-dir))

(defn full-sha
  "Takes a git-repo (as returned by ensure-git-dir) and a rev, and returns
  the full commit sha. rev may be a partial sha, full sha, or annotated tag name."
  ^String [git-repo rev]
  (if (ObjectId/isId rev)
    rev
    (.. (impl/git-repo (:git-dir git-repo)) (resolve rev) getName)))

(defn ancestor?
  "Checks whether ancestor-sha is an ancestor of child-sha. Both shas should be full shas."
  [child-git-repo ^String ancestor-sha ^String child-sha]
  (let [repo (-> child-git-repo :git-dir impl/git-repo)
        walk (RevWalk. repo)]
    (try
      (let [child-commit (.lookupCommit walk (ObjectId/fromString child-sha))
            ancestor-commit (.lookupCommit walk (ObjectId/fromString ancestor-sha))]
        (.isMergedInto walk ancestor-commit child-commit))
      (catch MissingObjectException e false)
      (finally (.dispose walk)))))

(comment
  (def git-repo (ensure-git-dir "https://github.com/clojure/spec.alpha.git"))

  (def wt (ensure-working-tree git-repo 'org.clojure/spec.alpha "739c1af5"))

  (def commit (full-sha git-repo "739c1af5"))

  (ancestor? git-repo commit commit)
  (ancestor? git-repo commit "739c1af56dae621aedf1bb282025a0d676eff712")
  )