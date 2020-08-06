(defproject server-timetracker "0.1.0-SNAPSHOT"
  :description "Tracking active time"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring "1.8.1"]
                 [compojure "1.6.1"]
                 [clj-time "0.15.2"]  ]
  :repl-options {:init-ns server-timetracker.core}
  :main ^:skip-aot server-timetracker.core
  :target-path "target/%s"
  :profile {:uberjar {:aot :all}}
  :plugins [[lein-codox "0.10.7"]
            [cider/cider-nrepl "0.25.0"]
            ]
  )
