(ns app.service.submit-service
  (:require [app.jira.api :as api]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [app.utils :as utils]
            ))

(defn collect-query [x]
  (for [temp x]
    (let [offset (:offset temp)
          date   (utils/formatlocal (-> temp :date) offset)
          start  (utils/formatlocal (c/to-string (t/with-time-at-start-of-day date)) offset)
          end    (utils/formatlocal (c/to-string (t/with-time-at-start-of-day (t/plus- date (t/days 1)))) offset)]
      (assoc temp
             :startDay (c/to-string start)
             :endDay (c/to-string end)))))

(defn submit-time [body]
  (api/load-resource
    {:endpoint :submit
     :method   :POST
     :body     (assoc body :query  (collect-query (:query body)))}))
