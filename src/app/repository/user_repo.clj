(ns app.repository.user-repo
  (:require [app.repository.pool :as pool]))

(def sqlmap {:select   [:start_date :end_date]
             :from     [:track_logs]
             :order-by [:id]
             :limit    10})

(def q-users
  {:select   [:jira_user_name :email]
   :from     [:users]
   :order-by [:id]
   :limit    10})

(defn load-users []
  (pool/execute sqlmap))

(comment
  (load-users)
  (pool/execute q-users)
  )

