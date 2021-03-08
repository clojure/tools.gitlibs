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

(set! *warn-on-reflection* true)

;; io util

(defn printerrln [& msgs]
  (binding [*out* *err*]
    (apply println msgs)))

(defn- runproc
  [{:keys [interactive print-commands]} & args]
  (when print-commands
    (apply printerrln args))
  (let [proc-builder (ProcessBuilder. ^java.util.List args)
        _ (when-not interactive (.put (.environment proc-builder) "GIT_TERMINAL_PROMPT" "0"))
        proc (.start proc-builder)
        exit (.waitFor proc)
        out (slurp (.getInputStream proc))
        err (slurp (.getErrorStream proc))]
    {:exit exit :out out :err err}))

;; dirs

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

(defn lib-dir
  ^File [lib]
  (jio/file (cache-dir) "libs" (namespace lib) (name lib)))

(defn- clean-url
  "Chop leading protocol, trailing .git, replace :'s with /"
  [url]
  (-> url
    (str/split #"://")
    last
    (str/replace #"\.git$" "")
    (str/replace #":" "/")))

(defn git-dir
  ^File [url]
  (jio/file (cache-dir) "_repos" (clean-url url)))

;; git clone --bare --quiet URL PATH
;; git --git-dir <> fetch
;; git --git-dir <> --work-tree <dst> checkout <rev>

(defn git-fetch
  [^File git-dir opts]
  (let [git-path (.getCanonicalPath git-dir)
        {:keys [exit err] :as ret} (runproc opts "git" "--git-dir" git-path "fetch" "--tags")]
    (when-not (zero? exit)
      (throw (ex-info (format "Unable to fetch %s%n%s" git-path err) ret)))))

;; TODO: restrict clone to an optional refspec?
(defn git-clone-bare
  [url ^File git-dir opts]
  (printerrln "Cloning:" url)
  (let [git-path (.getCanonicalPath git-dir)
        {:keys [exit err] :as ret} (runproc opts "git" "clone" "--bare" url git-path)]
    (when-not (zero? exit)
      (throw (ex-info (format "Unable to clone %s%n%s" git-path err) ret)))
    git-dir))

(defn ensure-git-dir
  "Ensure the bare git dir for the specified url, return the path to the git dir."
  [url opts]
  (let [git-dir-file (git-dir url)
        config-file (jio/file git-dir-file "config")]
    (when-not (.exists config-file)
      (git-clone-bare url git-dir-file opts))
    (.getCanonicalPath git-dir-file)))

(defn git-checkout
  [url ^File rev-dir ^String rev opts]
  (when-not (.exists rev-dir)
    (.mkdirs rev-dir))
  (runproc opts
    "git"
    "--git-dir" (ensure-git-dir url opts)
    "--work-tree" (.getCanonicalPath rev-dir)
    "checkout" rev))

(defn git-rev-parse
  [git-dir rev opts]
  (let [{:keys [exit out]} (runproc opts "git" "--git-dir" git-dir "rev-parse" rev)]
    (when (zero? exit)
      (str/trimr out))))

;; git merge-base --is-ancestor <maybe-ancestor-commit> <descendant-commit> 
(defn- ancestor?
  [git-dir x y opts]
  (let [args ["git" "--git-dir" git-dir "merge-base" "--is-ancestor" x y]
        {:keys [exit err] :as ret} (apply runproc opts args)]
    (condp = exit
      0 true
      1 false
      (throw (ex-info (format "Unable to compare commits %s%n%s" git-dir err) ret)))))

(defn commit-comparator
  [git-dir opts x y]
  (cond
    (= x y) 0
    (ancestor? git-dir x y opts) 1
    (ancestor? git-dir y x opts) -1
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
