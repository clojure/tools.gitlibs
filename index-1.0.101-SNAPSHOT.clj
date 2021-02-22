{:namespaces
 ({:doc
   "An API for retrieving and caching git repos and working trees.\n\nThe git url can be either an https url for anonymous checkout or an ssh url\nfor private access. revs can be either full sha, prefix sha, or tag name.",
   :name "clojure.tools.gitlibs",
   :wiki-url "https://clojure.github.io/tools.gitlibs/index.html",
   :source-url
   "https://github.com/clojure/tools.gitlibs/blob/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj"}),
 :vars
 ({:raw-source-url
   "https://github.com/clojure/tools.gitlibs/raw/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj",
   :name "cache-dir",
   :file "src/main/clojure/clojure/tools/gitlibs.clj",
   :source-url
   "https://github.com/clojure/tools.gitlibs/blob/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj#L23",
   :line 23,
   :var-type "function",
   :arglists ([]),
   :doc
   "Return the root gitlibs cache directory. By default ~/.gitlibs or\noverride by setting the environment variable GITLIBS.",
   :namespace "clojure.tools.gitlibs",
   :wiki-url
   "https://clojure.github.io/tools.gitlibs//index.html#clojure.tools.gitlibs/cache-dir"}
  {:raw-source-url
   "https://github.com/clojure/tools.gitlibs/raw/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj",
   :name "descendant",
   :file "src/main/clojure/clojure/tools/gitlibs.clj",
   :source-url
   "https://github.com/clojure/tools.gitlibs/blob/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj#L55",
   :line 55,
   :var-type "function",
   :arglists ([url revs]),
   :doc
   "Returns rev in git url which is a descendant of all other revs,\nor nil if no such relationship can be established.",
   :namespace "clojure.tools.gitlibs",
   :wiki-url
   "https://clojure.github.io/tools.gitlibs//index.html#clojure.tools.gitlibs/descendant"}
  {:raw-source-url
   "https://github.com/clojure/tools.gitlibs/raw/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj",
   :name "procure",
   :file "src/main/clojure/clojure/tools/gitlibs.clj",
   :source-url
   "https://github.com/clojure/tools.gitlibs/blob/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj#L41",
   :line 41,
   :var-type "function",
   :arglists ([url lib rev]),
   :doc
   "Procure a working tree at rev for the git url representing the library lib,\nreturns the directory path. lib is a qualified symbol where the qualifier is a\ncontrolled or conveyed identity, or nil if rev is unknown.",
   :namespace "clojure.tools.gitlibs",
   :wiki-url
   "https://clojure.github.io/tools.gitlibs//index.html#clojure.tools.gitlibs/procure"}
  {:raw-source-url
   "https://github.com/clojure/tools.gitlibs/raw/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj",
   :name "resolve",
   :file "src/main/clojure/clojure/tools/gitlibs.clj",
   :source-url
   "https://github.com/clojure/tools.gitlibs/blob/752cd0ca4dfa25d48d22770321b0ada56bb64557/src/main/clojure/clojure/tools/gitlibs.clj#L29",
   :line 29,
   :var-type "function",
   :arglists ([url rev]),
   :doc
   "Takes a git url and a rev, and returns the full commit sha. rev may be a\npartial sha, full sha, or tag name.",
   :namespace "clojure.tools.gitlibs",
   :wiki-url
   "https://clojure.github.io/tools.gitlibs//index.html#clojure.tools.gitlibs/resolve"})}
