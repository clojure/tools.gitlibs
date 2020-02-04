;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.test-gitlibs
  (:require
    [clojure.java.io :as jio]
    [clojure.test :refer :all]
    [clojure.tools.gitlibs :as gl]
    [clojure.tools.gitlibs.impl :as glim]))

(def repo-url "https://github.com/clojure/spec.alpha.git")

(deftest test-resolve
  (is (= (gl/resolve repo-url "739c1af5")
        "739c1af56dae621aedf1bb282025a0d676eff713")))

(deftest test-procure
  (let [wt (gl/procure repo-url 'org.clojure/spec.alpha "739c1af")]
    (is (= wt (.getAbsolutePath (jio/file (glim/cache-dir) "libs" "org.clojure" "spec.alpha" "739c1af56dae621aedf1bb282025a0d676eff713"))))
    (is (.exists (jio/file (glim/cache-dir) "_repos" "github.com" "clojure" "spec.alpha")))))

(deftest test-procure-with-submodules
  (let [repo-url "https://github.com/borkdude/babashka"
        rev "1224919bd19033386c732bdfd93f4b012e9da9c3"
        wt (gl/procure repo-url 'borkdude/babashka rev)]
    (is (= wt (.getAbsolutePath (jio/file (glim/cache-dir) "libs" "borkdude" "babashka" "1224919bd19033386c732bdfd93f4b012e9da9c3"))))
    (is (.exists (jio/file (glim/cache-dir) "_repos" "github.com" "borkdude" "babashka")))))

(deftest test-descendant-fixed
  (is (= (gl/descendant repo-url ["607aef0" "739c1af"])
        "739c1af56dae621aedf1bb282025a0d676eff713"))
  (is (= (gl/descendant repo-url ["739c1af" "607aef0"])
        "739c1af56dae621aedf1bb282025a0d676eff713"))
  (is (= (gl/descendant repo-url ["607aef0" "607aef0"])
        "607aef0643f6cf920293130d45e6160d93fda908"))

  (is (nil? (gl/descendant repo-url nil)))
  (is (nil? (gl/descendant repo-url [])))
  (is (nil? (gl/descendant repo-url ["abcdef" "123456"]))))

(deftest test-descendant-combos
  (let [m (gl/resolve repo-url "master")
        m' (gl/resolve repo-url "master~1")
        m'' (gl/resolve repo-url "master~2")]
    (are [rs d] (= d (gl/descendant repo-url rs))
                [m] m
                [m m''] m
                [m' m''] m'
                [m'' m' m] m)))