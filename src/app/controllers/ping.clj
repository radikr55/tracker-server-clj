(ns app.controllers.ping
  (:require   [app.routes :refer [multi-handler]]
              [app.responses :as responses]
              [app.repository.track-log-repo :as track-repo]
              [app.jira.api :as api]
              [clj-time.coerce :as c]
              [clj-time.core :as t]
              [clj-time.format :as f]
              [clojure.pprint :refer [pprint]]
              [app.repository.user-repo :as u]))

(defn get-interval [start end]
  (t/in-minutes (t/interval start end)))

(defn formatlocal [date-str offset]
  (let [format (f/formatter-local "yyyy-MM-dd HH:mm:ss")
        zone   (t/time-zone-for-offset (/ offset 60))
        resp   (t/to-time-zone (c/from-string date-str) zone)]
    (f/parse format (f/unparse format resp))))

(defmethod multi-handler :get-track-logs
  [request]
  (try
    (let [jira-user-name (api/load-resource {:endpoint :jira-user
                                             :body     (:body request)})
          offset         (-> request :body :offset)
          start          (formatlocal (-> request :body :start) offset)
          end            (formatlocal (-> request :body :end) offset)
          user-id        (-> (u/get-user-id jira-user-name)
                             first
                             :id)
          track-logs     (track-repo/load-track-logs start end user-id)
          task-codes     (->> track-logs
                              (map :task)
                              (into #{})
                              (filter #(not (empty? %)))
                              (map #(assoc {} :issueCode %)))
          issue-list     (->> (assoc  (:body request)
                                      :query {:list      task-codes
                                              :endDate   end
                                              :startDate start})
                              (assoc {:endpoint :issue
                                      :json     true
                                      :method   :POST} :body)
                              (load-from-jira))]
      (responses/ok (assoc {} :data track-logs :desc issue-list)))
    (catch Exception e
      (responses/error (ex-data e) e))))

(comment
  (def list '({:start_date #inst "2020-07-17T04:54:00.000000000-00:00",
               :end_date   #inst "2020-07-17T06:48:00.000000000-00:00",
               :task       "WELKIN-76"}
              {:start_date #inst "2020-07-17T08:04:00.000000000-00:00",
               :end_date   #inst "2020-07-17T08:06:00.000000000-00:00",
               :task       "WELKIN-76"}
              {:start_date #inst "2020-07-17T08:06:00.000000000-00:00",
               :end_date   #inst "2020-07-17T08:30:00.000000000-00:00",
               :task       ""}
              {:start_date #inst "2020-07-17T08:30:00.000000000-00:00",
               :end_date   #inst "2020-07-17T10:42:00.000000000-00:00",
               :task       "WELKIN-76"}
              {:start_date #inst "2020-07-17T10:42:00.000000000-00:00",
               :end_date   #inst "2020-07-17T10:45:00.000000000-00:00",
               :task       ""}))

  (let [task-codes (->> list
                        (map :task)
                        (into #{})
                        (filter #(not (empty? %)))
                        (map #(assoc {} :issueCode %)))
        issue-list (->> (assoc  {:token  "WZv45yWt0ReOJGAYEfJFh6e8B2nAXxrm"
                                 :secret "35494zhP2SmiGGNxo774lT8HNT5YJXwZ"}
                                :query {:list      task-codes
                                        :endDate   (t/date-time 2020 7 14 21)
                                        :startDate (t/date-time 2020 7 13 21)})
                        (assoc {:endpoint :issue
                                :json     true
                                :method   :POST} :body)
                        (load-from-jira))]
    (print (:issueWithSummary issue-list)))
  (load-from-jira {:endpoint :jira-user
                   :method   :GET
                   :body     {:token  "WZv45yWt0ReOJGAYEfJFh6e8B2nAXxrm"
                              :secret "35494zhP2SmiGGNxo774lT8HNT5YJXwZ"}}))

(defmethod multi-handler :save-ping
  [request]
  (try
    (let [body           (:body request)
          jira-user-name (api/load-resource {:endpoint :jira-user
                                             :body     body})
          user-id        (-> (u/get-user-id jira-user-name)
                             first
                             :id)
          offset         (:offset body)]
      (->> body
           :data
           (map #(assoc %
                        :start (formatlocal (:start %) offset)
                        :end (formatlocal (:end %) offset)
                        ;; :log_length (get-interval (:start %) (:end %))
                        :user_id user-id))
           pprint))
    (responses/ok {:test 123})
    (catch Exception e
      (responses/error (ex-data e) e))))

(defn collect-query [x]
  (for [temp x]
    (let [offset (:offset temp)
          date   (formatlocal (-> temp :date) offset)
          start  (formatlocal (c/to-string (t/with-time-at-start-of-day date)) offset)
          end    (formatlocal (c/to-string (t/with-time-at-start-of-day (t/plus- date (t/days 1)))) offset)]
      (assoc temp
             :startDay (c/to-string start)
             :endDay (c/to-string end)))))

(defmethod multi-handler :submit
  [request]
  (try
    (let [body  (:body request)
          query (collect-query (:query body))]
      (api/load-resource
        {:endpoint :submit
         :method   :POST
         :body     (assoc body
                          :query query)})
      (responses/ok {}))
    (catch Exception e
      (responses/error (ex-data e) e))))
