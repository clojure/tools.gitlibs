;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:skip-wiki true}
  clojure.tools.gitlibs.impl
  "Implementation, use at your own risk"
  (:require
    [clojure.java.io :as jio]
    [clojure.string :as str])
  (:import
    [java.io File FilenameFilter IOException]
    [org.eclipse.jgit.api Git GitCommand TransportCommand TransportConfigCallback]
    [org.eclipse.jgit.lib Repository RepositoryBuilder]
    [org.eclipse.jgit.revwalk RevWalk RevCommit]
    [org.eclipse.jgit.transport SshTransport JschConfigSessionFactory]
    [com.jcraft.jsch JSch]
    [com.jcraft.jsch.agentproxy Connector ConnectorFactory RemoteIdentityRepository]))

(defn printerrln [& msgs]
  (binding [*out* *err*]
    (apply println msgs)))

(def ^:private ^TransportConfigCallback ssh-callback
  (delay
    (let [factory (doto (ConnectorFactory/getDefault) (.setPreferredUSocketFactories "jna,nc"))
          connector (.createConnector factory)]
      (JSch/setConfig "PreferredAuthentications" "publickey")
      (reify TransportConfigCallback
        (configure [_ transport]
          (.setSshSessionFactory ^SshTransport transport
            (proxy [JschConfigSessionFactory] []
              (configure [host session])
              (getJSch [hc fs]
                (doto (proxy-super getJSch hc fs)
                  (.setIdentityRepository (RemoteIdentityRepository. connector)))))))))))

(defn- call-with-auth
  ([^GitCommand command]
    (call-with-auth
      (.. command getRepository getConfig (getString "remote" "origin" "url"))
      command))
  ([^String url ^GitCommand command]
   (if (and (instance? TransportCommand command)
         (not (str/starts-with? url "http")))
     (.. ^TransportCommand command (setTransportConfigCallback @ssh-callback) call)
     (.call command))))

(defn git-repo
  (^Repository [git-dir]
   (.build (.setGitDir (RepositoryBuilder.) (jio/file git-dir))))
  (^Repository [git-dir rev-dir]
   (.build
     (doto (RepositoryBuilder.)
       (.setGitDir (jio/file git-dir))
       (.setWorkTree (jio/file rev-dir))))))

(defn git-fetch
  ^Git [git-dir]
  (let [git (Git. (git-repo git-dir))]
    (call-with-auth (.. git fetch))
    git))

;; TODO: restrict clone to an optional refspec?
(defn git-clone-bare
  [url git-dir]
  (printerrln "Cloning:" url)
  (call-with-auth url
    (.. (Git/cloneRepository) (setURI url) (setGitDir (jio/file git-dir))
      (setCloneSubmodules true)
      (setBare true)
      (setNoCheckout true)
      (setCloneAllBranches true)))
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
      (try
        (git-fetch git-dir)
        (catch Throwable _
          ;; if can't fetch, local cache may be corrupt, try recloning
          (git-clone-bare url git-dir)))
      (git-clone-bare url git-dir))
    (.getCanonicalPath git-dir)))

(defn git-checkout
  [url rev-dir ^String rev]
  (let [git-dir (ensure-git-dir url)
        git (Git. (git-repo git-dir rev-dir))]
    (call-with-auth (.. git checkout (setStartPoint rev) (setAllPaths true)))))

(defn commit-comparator
  [^RevWalk walk ^RevCommit x ^RevCommit y]
  (cond
    (= x y) 0
    (.isMergedInto walk x y) 1
    (.isMergedInto walk y x) -1
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
