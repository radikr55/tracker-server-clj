(ns app.repository.active-task-repo
  (:require [app.repository.pool :as pool]
            [app.repository.utils :as utils]
            [clj-time.core :as t]))

(defn load-active-tasks [user-id]
  (pool/query {:select [:*]
               :from   [:active_tasks]
               :where  [:= :client_id user-id]}))

(defn delete
  ([id]             (pool/execute {:delete-from [:active_tasks :t]
                                   :where       [:= :t.id id]}))
  ([user-id code]   (pool/execute {:delete-from [:active_tasks :t]
                                   :where       [:and
                                                 [:= :t.client_id user-id]
                                                 [:= :t.task code]]})))


(defn save [user-id code]
  (let [create (utils/to-sql-date (t/now))
        id     (pool/nextval "active_tasks")
        record {:id           id
                :created_date create
                :client_id    user-id
                :task         code}]
    (pool/execute {:insert-into :active_tasks
                   :values      [record]})))

(comment
  (save 166 "test")

  (delete 166 "WELKIN-9"))
