(ns app.controllers.login
  (:require
   [clojure.pprint :as pp]
   [clj-http.client :as client]
   [app.responses :as responses]
   [app.repository.redis :as redis]
   [app.routes :refer [multi-handler]]
   [app.jira.auth :as auth])
  (:import java.util.Base64))

(defn get-oauth-url [{:keys [login password]}]
  (let [token         (auth/request-token)
        request-token (:oauth_token token)]
    (auth/get-user login password)
    {:request-token request-token
     :url           (auth/get-approval-url token)}))

(defn oauth-callback [request]
  (let [verifier      (:oauth-verifier request)
        request-token {:oauth_token (:oauth-token request)}
        access-token  (auth/access-token-response verifier request-token)]
    {:status 200
     :body   access-token}))

(defmethod multi-handler :login
  [request]
  (try
    (responses/ok (get-oauth-url (:body request)))
    (catch Exception e
      (responses/error (ex-data e)))))

(defmethod multi-handler :oauth
  [request]
  (responses/ok (oauth-callback (:body request))))

(comment
  (def token (auth/request-token))
  (auth/get-approval-url token)
  (defn auth-jira [request]
    (let [login (:login (:body request))
          pass  (:password (:body request))
          auth  (encode (format "%s:%s" login pass))
          url   (auth/get-approval-url token)]
      (client/get url {:headers {"Authorization" (str "Basic " auth)} :debug true})))
  (defn encode [to-encode]
    (.encodeToString (Base64/getEncoder) (.getBytes to-encode)))
  (encode "123")
  (auth-jira {:body {:login "admin" :password "admin"}}))
