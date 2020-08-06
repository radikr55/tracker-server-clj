(ns app.controllers.project
  (:require
   [clj-http.client :as client]
   [app.routes :refer [multi-handler]]
   [app.responses :as responses]
   [app.jira.auth :as auth]))

(defn get-project [body]
  (let [token       (:token body)
        secret      (:secret body)
        url         auth/jira-projects
        credentials (auth/get-credentials token secret url)]
    (client/get url
                {:query-params credentials
                 :debug        true})))

(defmethod multi-handler :project
  [request]
  (try
    (responses/ok (get-project (:body request )))
    (catch Exception e
      (responses/error (ex-data e)))))

