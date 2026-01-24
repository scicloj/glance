(ns build
  "Glance's build script.

  clojure -T:build jar
  clojure -T:build deploy"
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'org.scicloj/glance)
(def version "0.1.0-alpha1")
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn- pom-template [version]
  [[:description "Just plot your data"]
   [:url "https://scicloj.github.io/glance/"]
   [:licenses
    [:license
     [:name "Eclipse Public License - v 2.0"]
     [:url "https://www.eclipse.org/legal/epl-2.0/"]]]
   [:scm
    [:url "https://github.com/scicloj/glance"]
    [:connection "scm:git:https://github.com/scicloj/glance.git"]
    [:developerConnection "scm:git:ssh:git@github.com:scicloj/glance.git"]
    [:tag (str "v" version)]]])

(defn jar "Build the JAR." [opts]
  (b/write-pom {:lib       lib
                :version   version
                :class-dir class-dir
                :basis     (b/create-basis {})
                :pom-data  (pom-template version)})
  (b/copy-dir {:src-dirs   ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file}))

(defn tag "Set Git TAG for version." [_]
  (b/git-process {:git-args ["tag" "-a" (str "v" version)
                             "-m" (str "Release version v" version)]})
  (b/git-process {:git-args ["push" "origin" (str "v" version)]}))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (tag nil)
  (dd/deploy {:installer :remote
              :artifact  (b/resolve-path jar-file)
              :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))})
  opts)
