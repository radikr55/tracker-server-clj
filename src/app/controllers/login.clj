(ns app.controllers.login
  (:require [app.responses :as responses]
            [app.routes :refer [multi-handler]]
            [app.jira.auth :as auth]
            [app.jira.api :as api]))

(defn get-oauth-url []
  (let [token         (auth/request-token)
        request-token (:oauth_token token)]
    {:request-token request-token
     :url           (auth/get-approval-url token)}))

(defn oauth-callback [request]
  (let [verifier      (:oauth-verifier request)
        request-token {:oauth_token (:oauth-token request)}
        access-token  (auth/access-token-response verifier request-token)]
    {:status 200
     :body   access-token}))

(defmethod multi-handler :auth-link [_]
  (try
    (responses/ok (get-oauth-url))
    (catch Exception e
      (responses/error e (ex-data e)))))

(defmethod multi-handler :oauth
  [request]
  (responses/ok (oauth-callback (:body request))))

(defmethod multi-handler :user-name
  [request]
  (responses/ok {:name (api/load-resource {:body     (:body request )
                                           :endpoint :jira-user}) }))
