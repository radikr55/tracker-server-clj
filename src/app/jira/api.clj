(ns app.jira.api
  (:require [app.config :refer [config]]
            [oauth.client :as oauth]
            [cheshire.core :as json]
            [app.jira.auth :as auth]
            [clj-http.client :as client]))

(def jira-plugin-url (str (-> config :jira :host) (-> config :jira :plugin-uri)))

(defmulti ->endpoint (fn [id] id))

(defmethod ->endpoint :jira-user [_]
  "/user")

(defmethod ->endpoint :jira-projects [_]
  "/projectsWithTasks")

(defmethod ->endpoint :jira-tasks [_]
  "/tasks")

(defmethod ->endpoint :issue [_]
  "/issue")

(defmethod ->endpoint :submit [_]
  "/issue/submit")

(defmethod ->endpoint :jira-by-project-id [_]
  "/projects/tasks")

(defn ->url [endpoint]
  (str jira-plugin-url (->endpoint endpoint)))

(defn ->method [method]
  (case method
    :POST client/post
    client/get))

(defn ->credentials
  ([token secret endpoint] (oauth/credentials auth/consumer
                                              token
                                              secret
                                              :GET
                                              (->url endpoint)))
  ([token secret endpoint method] (oauth/credentials auth/consumer
                                                     token
                                                     secret
                                                     method
                                                     (->url endpoint))))

(defn fetch [{:keys [token secret endpoint method query debug params json]}]
  (let [meth         (->method method)
        url          (->url endpoint)
        query-params (if token (->credentials token secret endpoint method)
                         params)
        resp         (meth url {:query-params query-params
                                :form-params  query
                                :content-type :json
                                :debug        debug})]
    (if json
      (json/parse-string (:body resp) true)
      (:body resp))))

(defn load-resource [{:keys [body endpoint method json]}]
  (let [token  (:token body)
        secret (:secret body)
        method (or method :GET)
        query  (or (:query body) {})]
    (fetch {:method   method
            :token    token
            :secret   secret
            :endpoint endpoint
            :json     json
            :query    query})))

