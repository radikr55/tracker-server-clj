(ns app.controllers.project
  (:require
   [clj-http.client :as client]
   [app.routes :refer [multi-handler]]
   [app.responses :as responses]
   [app.jira.auth :as auth]))

(defn load-resource [body url]
  (let [token       (:token body)
        secret      (:secret body)
        query       (or (:query body) {})
        credentials (auth/get-credentials token secret url :POST)]
    (client/post url
                 {:query-params credentials
                  :form-params  query
                  :content-type :json
                  :debug        true}))
  )

(defmethod multi-handler :project
  [request]
  (try
    (responses/ok (load-resource (:body request) auth/jira-projects))
    (catch Exception e
      (responses/error (ex-data e)))))


(defmethod multi-handler :tasks
  [request]
  (try
    (responses/ok (load-resource (:body request) auth/jira-tasks))
    (catch Exception e
      (responses/error (ex-data e)))))
