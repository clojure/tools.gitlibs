(ns clojure.tools.gitlibs.impl
  "Implementation, use at your own risk"
  (:require
    [clojure.java.io :as jio]
    [clojure.string :as str])
  (:import
    [java.io File]
    [org.eclipse.jgit.api Git GitCommand TransportCommand TransportConfigCallback]
    [org.eclipse.jgit.lib Repository RepositoryBuilder]
    [org.eclipse.jgit.transport SshTransport JschConfigSessionFactory]
    [com.jcraft.jsch JSch]
    [com.jcraft.jsch.agentproxy Connector ConnectorFactory RemoteIdentityRepository]))

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
  ([command]
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
  ^Git [git-dir rev-dir]
  (let [git (Git. (git-repo git-dir rev-dir))]
    (call-with-auth (.. git fetch))
    git))

;; TODO: restrict clone to an optional refspec?
(defn git-clone-bare
  [url git-dir]
  (call-with-auth url
    (.. (Git/cloneRepository) (setURI url) (setGitDir (jio/file git-dir))
      (setBare true)
      (setNoCheckout true)
      (setCloneAllBranches true)))
  git-dir)

(defn git-checkout
  [^Git git ^String rev]
  (call-with-auth (.. git checkout (setStartPoint rev) (setAllPaths true))))

(def cache-dir
  (.getAbsolutePath (jio/file (System/getProperty "user.home") ".gitlibs")))

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
  (let [git-dir (jio/file cache-dir "_repos" (clean-url url))]
    (when-not (.exists git-dir)
      (git-clone-bare url git-dir))
    (.getCanonicalPath git-dir)))