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
    [clojure.string :as str])
  (:import
    [java.io File]
    [org.eclipse.jgit.api Git GitCommand TransportCommand TransportConfigCallback]
    [org.eclipse.jgit.lib Repository RepositoryBuilder ObjectId]
    [org.eclipse.jgit.revwalk RevWalk]
    [org.eclipse.jgit.transport SshTransport JschConfigSessionFactory]
    [org.eclipse.jgit.errors MissingObjectException]
    [com.jcraft.jsch JSch]
    [com.jcraft.jsch.agentproxy Connector ConnectorFactory RemoteIdentityRepository]))

;;;; Git

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

(defn- call-with
  [^String url ^GitCommand command]
  (if (and (instance? TransportCommand command)
        (not (str/starts-with? url "http")))
    (.. ^TransportCommand command (setTransportConfigCallback @ssh-callback) call)
    (.call command)))

(defn- clean-url
  "Chop leading protocol, trailing .git, replace :'s with /"
  [url]
  (-> url
    (str/split #"://")
    last
    (str/replace #"\.git$" "")
    (str/replace #":" "/")))

(defn git-repo
  ^Repository [{:keys [^File git-dir ^File rev-dir]}]
  (.. (cond-> (RepositoryBuilder.)
        git-dir (.setGitDir git-dir)
        rev-dir (.setWorkTree rev-dir))
    build))

(defn- git-fetch
  ^Git [dirs ^String url]
  (let [git (Git. (git-repo dirs))]
    (call-with url (.. git fetch))
    git))

;; TODO: restrict clone to an optional refspec?
(defn- git-clone-bare
  ^File [^String url ^File git-dir]
  (call-with url
    (.. (Git/cloneRepository) (setURI url) (setGitDir ^File git-dir)
      (setBare true)
      (setNoCheckout true)
      (setCloneAllBranches true)))
  git-dir)

(defn- git-checkout
  [^Git git ^String rev ^String url]
  (call-with url (.. git checkout (setStartPoint rev) (setAllPaths true))))

(defn full-commit
  ^String [^File git-dir ^String rev]
  (if (ObjectId/isId rev)
    rev
    (.. (git-repo {:git-dir git-dir}) (resolve rev) getName)))

(defn parent?
  [^String parent ^String child dirs]
  (let [repo (git-repo dirs)
        walk (RevWalk. repo)]
    (try
      (let [child-commit (.lookupCommit walk (ObjectId/fromString child))
            parent-commit (.lookupCommit walk (ObjectId/fromString parent))]
        (.isMergedInto walk parent-commit child-commit))
      (catch MissingObjectException e false)
      (finally (.dispose walk)))))

(defn ensure-git-dir
  "Download the bare git dir for the specified url, return the cached git dir"
  ^File [^String cache-dir ^String url]
  (let [git-dir (jio/file cache-dir "_repos" (clean-url url))]
    (if (.exists git-dir)
      git-dir
      (git-clone-bare url git-dir))))

(defn ensure-rev-dir
  "Download working tree for the specified url, return the cached rev dir"
  [lib ^String cache-dir ^String url ^String rev]
  (let [git-dir (ensure-git-dir cache-dir url)
        rev-dir (jio/file cache-dir "libs" (namespace lib) (name lib) rev)
        dirs {:git-dir git-dir, :rev-dir rev-dir}]
    (when (not (.exists rev-dir))
      (git-checkout (git-fetch dirs url) rev url))
    dirs))

(comment
  (def dirs (#'ensure-rev-dir 'org.clojure/spec.alpha
              (File. "/Users/alex/code/.clojure/.cpcache")
              "https://github.com/clojure/spec.alpha.git"
              "739c1af56dae621aedf1bb282025a0d676eff713"))

  )
