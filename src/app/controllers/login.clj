(ns app.controllers.login
  (:require
   [clojure.pprint :as pp]
   [clj-http.client :as client]
   [app.responses :as responses]
   [app.repository.redis :as redis]
   [app.routes :refer [multi-handler]]
   [app.jira.auth :as auth]
   [app.jira.api :as api])
  (:import java.util.Base64))


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

(defmethod multi-handler :auth-link
  [request]
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


(comment
  (api/load-resource {:body     {:token  "VEwUw4O1cRZibUhWyRd270Rs7jofOn1V"
                                 :secret "yl4H8kJuA2BlNI8fAyGLlrveTXR1mGYj"}
                      :method   :POST
                      :endpoint :jira-user})
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
