(defproject tracker-server-clj "0.1.0-SNAPSHOT"
  :description "Tracking active time"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [ring/ring-json "0.5.0"]
                 [ring "1.8.1"]
                 [bidi "2.1.6"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [clj-http "2.3.0"]
                 [raven-clj "1.6.0"]
                 [clj-time "0.15.2"]
                 [ring.middleware.logger "0.5.0"]
                 [cheshire "5.10.0"]
                 ;; [environ "1.2.0"]
                 [clj-oauth "1.5.5"]
                 [oauth-clj "0.1.16"]
                 [aero "1.1.6"]
                 [com.taoensso/carmine "2.19.1"]
                 [honeysql "1.0.444"]
                 [metabase/honeysql-util "1.0.2"]
                 [org.postgresql/postgresql "42.1.4"]
                 [org.bouncycastle/bcprov-jdk15on "1.64"]
                 [org.bouncycastle/bcpkix-jdk15on "1.64"]
                 [com.google.oauth-client/google-oauth-client "1.30.6"]
                 [com.google.http-client/google-http-client "1.35.0"]]
  :jvm-opts ["-Duser.timezone=UTC"]
  :repl-options {:init-ns app.core}
  :aot :all
  :main ^:skip-aot app.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:env {:DBPORT 3333}}}
  :plugins [[lein-codox "0.10.7"]
            [cider/cider-nrepl "0.25.0"]
            ;; [lein-environ "1.2.0"]
            [lein-ubersource "0.1.1"]
            [lein-plantuml "0.1.22"]]
  :codox {:output-path "doc/codox"
          :doc-files   ["doc/topics/intro.md", "doc/topics/test.md"]
          :metadata    {:doc/format :markdown}}
  :plantuml [["resources/*.puml" :png "doc/images"]
             ["presentation/*.txt" "svg"]])
