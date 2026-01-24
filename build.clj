(ns build
  "Glance's build script.

  clojure -T:build jar
  clojure -T:build ci
  clojure -T:build deploy"
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'org.scicloj/glance)
(def version "0.1.0-alpha1")
(def class-dir "target/classes")

(defn- pom-template [version]
  [[:description "Glance: automatic visualization geometry selection for Clojure data."]
   [:url "https://scicloj.github.io/glance/"]
   [:licenses
    [:license
     [:name "Eclipse Public License - v 2.0"]
     [:url "https://www.eclipse.org/legal/epl-2.0/"]]]
   [:scm
    [:url "https://github.com/scicloj/glance"]
    [:connection "scm:git:https://github.com/scicloj/glance.git"]
    [:developerConnection "scm:git:ssh:git@github.com:scicloj/glance.git"]]])

(defn jar "Build the JAR." [opts]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     (b/create-basis {})
                :src-pom   "pom-template.xml"
                :pom-data  (pom-template version)})
  (b/copy-dir {:src-dirs   ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  (format "target/%s-%s.jar" (name lib) version)}))

(defn ci "Build the JAR (CI pipeline)." [opts]
  (jar opts))

(defn deploy "Deploy to Clojars." [opts]
  (let [version version]
    (dd/deploy {:installer :remote
                :artifact  (format "target/%s-%s.jar" (name lib) version)
                :pom-file  (format "pom.xml")}))
  opts)
