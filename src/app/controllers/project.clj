(ns app.controllers.project
  (:require
   [clj-http.client :as client]
   [app.routes :refer [multi-handler]]
   [app.responses :as responses]
   [app.jira.api :as api]
   [app.jira.auth :as auth]))

(defmethod multi-handler :project
  [request]
  (try
    (responses/ok (api/load-resource {:body     (:body request)
                                      :method   :POST
                                      :endpoint :jira-projects}))
    (catch Exception e
      (responses/error (ex-data e) e))))

(defmethod multi-handler :tasks
  [request]
  (try
    (responses/ok (api/load-resource {:body     (:body request)
                                      :method   :POST
                                      :endpoint :jira-tasks}))
    (catch Exception e
      (responses/error (ex-data e) e))))

(defmethod multi-handler :by-project-id
  [request]
  (try
    (if (-> request :body :query :key)
      (responses/ok (api/load-resource {:body     (:body request)
                                        :method   :POST
                                        :endpoint :jira-by-project-id}))
      (responses/ok (api/load-resource {:body     (dissoc (:body request) :query)
                                        :method   :POST
                                        :endpoint :jira-projects})))
    (catch Exception e
      (responses/error (ex-data e) e))))
