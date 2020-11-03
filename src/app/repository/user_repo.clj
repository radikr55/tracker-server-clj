(ns app.repository.user-repo
  (:require [app.repository.pool :as pool]))

(defn get-user-id [user-name]
  (pool/query {:select   [:id]
               :from     [:users]
               :order-by [:id]
               :where    [:= :jira_user_name user-name]}))


