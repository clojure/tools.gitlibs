;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.gitlibs.impl
  "Implementation, use at your own risk"
  (:require
    [clojure.java.io :as jio]
    [clojure.string :as str])
  (:import
    [java.io File FilenameFilter IOException]))

(defn printerrln [& msgs]
  (binding [*out* *err*]
    (apply println msgs)))

(defn- runproc
  [& args]
  (let [proc (.start (ProcessBuilder. ^java.util.List args))
        code (.waitFor proc)
        out (slurp (.getInputStream proc))
        err (slurp (.getErrorStream proc))]
    (when-not (zero? code)
      (apply printerrln args)
      (printerrln err))
    {:exit code
     :out out
     :err err}))

;; git clone --bare --quiet URL PATH
;; git --git-dir <> fetch
;; git --git-dir <> --work-tree <dst> checkout <rev>

(defn git-fetch
  [^File git-dir]
  (runproc "git" "--git-dir" (.getCanonicalPath git-dir) "fetch"))

;; TODO: restrict clone to an optional refspec?
(defn git-clone-bare
  [url ^File git-dir]
  (printerrln "Cloning:" url)
  (runproc "git" "clone" "--bare" url (.getCanonicalPath git-dir))
  git-dir)

(def ^:private CACHE
  (delay
    (.getCanonicalPath
      (let [env (System/getenv "GITLIBS")]
        (if (str/blank? env)
          (jio/file (System/getProperty "user.home") ".gitlibs")
          (jio/file env))))))

(defn cache-dir
  "Absolute path to the root of the cache"
  []
  @CACHE)

(defn- clean-url
  "Chop leading protocol, trailing .git, replace :'s with /"
  [url]
  (-> url
    (str/split #"://")
    last
    (str/replace #"\.git$" "")
    (str/replace #":" "/")))

(defn ensure-git-dir
  "Ensure the bare git dir for the specified url."
  [url]
  (let [git-dir (jio/file (cache-dir) "_repos" (clean-url url))]
    (if (.exists git-dir)
      (git-fetch git-dir)
      (git-clone-bare url git-dir))
    (.getCanonicalPath git-dir)))

(defn git-checkout
  [url ^File rev-dir ^String rev]
  (when-not (.exists rev-dir)
    (.mkdirs rev-dir))
  (runproc "git"
           "--git-dir" (ensure-git-dir url)
           "--work-tree" (.getCanonicalPath rev-dir)
           "checkout" rev))

(defn git-rev-parse
  [git-dir rev]
  (let [p (runproc "git" "--git-dir" git-dir "rev-parse" rev)]
    (when (zero? (:exit p))
      (str/trimr (:out p)))))

;; git merge-base --is-ancestor <maybe-ancestor-commit> <descendant-commit> 
(defn- ancestor?
  [git-dir x y]
  (let [args  ["git" "--git-dir" git-dir "merge-base" "--is-ancestor" x y]
        proc (.start (ProcessBuilder. ^java.util.List args))
        code (.waitFor proc)]
    (when-not (#{0 1} code)
      (throw (ex-info "" {})))
    (condp = code
      0 true
      1 false
      (throw (Exception. "")))))

(defn commit-comparator
  [git-dir x y]
  (cond
    (= x y) 0
    (ancestor? git-dir x y) 1
    (ancestor? git-dir y x) -1
    :else (throw (ex-info "" {}))))

(defn match-exact
  "In dir, match file in dir with exact, nil if doesn't exist"
  [^File dir exact]
  (when (.exists (jio/file dir exact))
    exact))

(defn match-prefix
  "In dir, match file in dir with prefix, nil if not found, exception if more than one."
  [^File dir prefix]
  (when (.exists dir)
    (if (.exists (jio/file dir prefix))
      prefix
      (let [matches (.listFiles dir
                      (reify FilenameFilter
                        (accept [_this _dir name]
                          (str/starts-with? name prefix))))]
        (case (alength matches)
          0 nil
          1 (.getName ^File (aget matches 0))
          (throw (IOException. (str "Prefix not unique: " prefix))))))))
