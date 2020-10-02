(ns app.repository.user-repo
  (:require [app.repository.pool :as pool]))

(def sqlmap {:select   [:start_date :end_date]
             :from     [:track_logs]
             :order-by [:id]
             :limit    10})

(defn get-user-id [user-name]
  (pool/execute {:select   [:id]
                 :from     [:users]
                 :order-by [:id]
                 :where    [:= :jira_user_name user-name]}))

(defn load-users []
  (pool/execute sqlmap))

(comment
  (load-users)
  (pool/execute q-users))

