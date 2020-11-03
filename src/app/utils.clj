(ns app.utils
  (:require [metabase.util.honeysql :as util]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]))

(defn to-sql-date [date]
  (util/->timestamp (c/to-string date)))

(defn get-interval [start end]
  (t/in-minutes (t/interval start end)))

(defn formatlocal [date-str offset]
  (let [format (f/formatter-local "yyyy-MM-dd HH:mm:ss")
        zone   (t/time-zone-for-offset (/ offset 60))
        resp   (t/to-time-zone (c/from-string date-str) zone)]
    (f/parse format (f/unparse format resp))))

(defn format-date [date]
  (let [format (f/formatter-local "yyyy-MM-dd HH:mm:ss")]
    (f/parse format date)))
