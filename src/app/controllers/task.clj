(ns app.controllers.task
  (:require   [app.routes :refer [multi-handler]]
              [app.responses :as responses]
              [app.repository.active-task-repo :as active-repo]
              [app.repository.track-log-repo :as track-repo]
              [app.service.track-log-service :as service]
              [clojure.pprint :refer [pprint]]
              [clj-time.coerce :as c]
              [app.utils :as utils]))

(defmethod multi-handler :delete-track-logs
  [{:keys [body user-id]}]
  (try
    (let [code       (:task  body)
          offset     (:offset body)
          start      (utils/formatlocal (:start body) offset)
          end        (utils/formatlocal (:end body) offset)
          track-logs (->> (track-repo/load-track-logs start end user-id code)
                          (map #(merge {} {:start        (c/from-sql-time (:start_date %))
                                           :end          (c/from-sql-time (:end_date %))
                                           :inactive_log (:inactive_log %)
                                           :uuid         (.toString (java.util.UUID/randomUUID))
                                           :status       (if (:inative_log %) "inactive" "active")
                                           :task         ""
                                           :log_length   (:log_length %)
                                           :user_id      user-id})))]
      (pprint track-logs)
      ;; (pprint start)
      ;; (pprint end)
      (doseq [log track-logs]
        (service/save-track-logs user-id log))
      ;; (active-repo/delete user-id code)
      (responses/ok {:code code}))
    (catch Exception e
      (responses/error (ex-data e) e))))

(comment
  (multi-handler {:handler :delete-track-logs
                  :user-id 166
                  :body    {:name   "r.shylo",
                            :token  "o4zAKoL9twxHwUIN0zMTc04P38fAC8tP",
                            :secret "aS3BBfU1TuMIZRdp6UGmcQVkOZdgweg3",
                            :offset -120,
                            :task   "WELKIN-9",
                            :start  "2020-11-01T00:00:00.000Z",
                            :end    "2020-11-02T00:00:00.000Z"}}))

(defmethod multi-handler :save-track-logs
  [{:keys [body user-id]}]
  (try
    (let [task-list (->> (active-repo/load-active-tasks user-id)
                         (map :task)
                         (into []))
          code      (-> body :query :code)]
      (when (not (contains? task-list code))
        (active-repo/save user-id code))
      (responses/ok {:code code}))
    (catch Exception e
      (responses/error (ex-data e) e))))

