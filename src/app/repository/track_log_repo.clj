(ns app.repository.track-log-repo
  (:require [app.repository.pool :as pool]
            [metabase.util.honeysql :as util]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn load-track-logs [start end user-id]
  (pool/execute {:select [:start_date :end_date :task]
                 :from   [:track_logs]
                 :where  [:and
                          [:= :user_id user-id]
                          [:>= :start_date (util/->timestamp (c/to-string start))]
                          [:<= :end_date  (util/->timestamp (c/to-string end))]]}))

(comment
  (util/->timestamp (c/to-sql-date (t/date-time 2020 7 13 21)))

  (c/from-sql-date (:start_date (count (load-track-logs
                                         (t/date-time 2020 7 13 21)
                                         (t/date-time 2020 7 14 21)
                                         166))))
  )
