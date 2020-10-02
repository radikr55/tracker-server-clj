(ns app.repository.pool
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource))
  (:require   [honeysql.core :as sql]
              [app.config :refer [config]]
              [clojure.java.jdbc :as jdbc]
              [honeysql.helpers :refer :all]))

(defn pool  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setUser (:user spec))
               (.setJdbcUrl (:host spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections 60)
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime 60))]
    {:datasource cpds}))

(def db-spec
  {:classname "org.postgresql.Driver"
   :host      (:host (:db config))
   :user      (:user (:db config))
   :password  (:password (:db config))})

(def pooled-db (delay (pool db-spec)))

(defn db-connection [] @pooled-db)

(defn execute [sqlmap]
  (let [sql (sql/format sqlmap :quoting :ansi)]
    (jdbc/query (db-connection) sql)))

(comment
  db-spec
  (def test {:select   [:*]
             :from     [:users]
             :order-by [:id]
             :limit    10})
  (execute test))
