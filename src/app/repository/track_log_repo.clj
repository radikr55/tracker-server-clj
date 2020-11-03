(ns app.repository.track-log-repo
  (:require [app.repository.pool :as pool]
            [app.repository.utils :as utils]))

(defn load-track-logs
  ([start end user-id task] (pool/query {:select [:*]
                                         :from   [:track_logs]
                                         :where  [:and
                                                  [:= :user_id user-id]
                                                  [:= :task task]
                                                  [:>= :start_date (utils/to-sql-date start)]
                                                  [:<= :end_date  (utils/to-sql-date end)]]}))
  ([start end user-id] (pool/query {:select [:*]
                                    :from   [:track_logs]
                                    :where  [:and
                                             [:= :user_id user-id]
                                             [:>= :start_date (utils/to-sql-date start)]
                                             [:<= :end_date  (utils/to-sql-date end)]]})))

(defn load-intersection [start end user-id]
  (let [start (utils/to-sql-date start)
        end   (utils/to-sql-date end)]
    (pool/query {:select   [:*]
                 :from     [:track_logs]
                 :where    [:and
                            [:= :user_id user-id]
                            [:or
                             [:and
                              [:>= :start_date start]
                              [:<= :end_date end]]
                             [:and
                              [:<= :start_date end]
                              [:> :end_date end]]
                             [:and
                              [:>= :end_date start]
                              [:< :start_date start]]]]
                 :order-by [:start_date]})))

(defn save [row]
  (let [start  (utils/to-sql-date (:start row))
        end    (utils/to-sql-date (:end row))
        create (utils/to-sql-date (:create-date row))
        id     (pool/nextval "track_logs")
        record (-> row
                   (assoc :id id
                          :start_date start
                          :end_date end
                          :created_date create)
                   (dissoc :start :end :create-date :status))]
    (when (not= 0 (:log_length row))
      (pool/execute {:insert-into :track_logs
                     :values      [record]}))))

(defn save-exist [row]
  (let [id     (pool/nextval "track_logs")
        record (-> (assoc row :id id)
                   (dissoc :status))]
    (pool/execute {:insert-into :track_logs
                   :values      [record]})))

(defn update-field [row]
  (let [id      (:id row)
        columns (:columns row)]
    (if (= 0 (:log_length columns))
      (pool/execute {:delete-from [:track_logs :t]
                     :where       [:= :t.id id]})
      (pool/execute {:update :track_logs
                     :set0   columns
                     :where  [:= :id id]}))))

(defn delete-log [id]
  (pool/execute {:delete-from [:track_logs :t]
                 :where       [:= :t.id id]}))

(comment
  ;; (update-field {:column :start_date
  ;;                :id     1053858
  ;;                :date   (t/date-time 2020 10 17 7 50)})
  (pool/execute {:delete-from [:track_logs :t]
                 :where       [:= :t.id 1055945]})

  ;; (util/->timestamp (c/to-sql-date (t/date-time 2020 7 13 21)))
  ;; (c/from-sql-date (:start_date (count (load-track-logs
  ;;                                        (t/date-time 2020 7 13 21)
  ;;                                        (t/date-time 2020 7 14 21)
  ;;                                        166))))
  (save {:start_date 123}))
