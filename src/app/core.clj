(ns app.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :as bidi]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [app.routes :refer [routes multi-handler]]
            [app.jira.api :as api]
            [app.repository.user-repo :as u]
            [app.controllers.login :as login]
            [app.controllers.project :as project]
            [app.controllers.ping :as ping]
            [app.controllers.task :as task]))

(def white-list     #{:auth-link
                      :user-name
                      :oauth
                      :tasks
                      :project
                      :by-project-id})

(extend-protocol cheshire.generate/JSONable
  org.joda.time.DateTime
  (to-json [dt gen]
    (cheshire.generate/write-string gen (str dt))))

(defn wrap-handler
  [handler]
  (fn [request]
    (let [{:keys [uri]} request
          request*      (bidi/match-route*
                          routes uri request)]
      (time (handler request*)))))

(defn wrap-user
  [handler]
  (fn [request]
    (if (contains? white-list (:handler request))
      (handler request)
      (let [jira-user-name (api/load-resource {:endpoint :jira-user
                                               :body     (:body request)})
            user-id        (-> (u/get-user-id jira-user-name)
                                first
                                :id)]
        (handler (assoc request
                        :user-id        user-id
                        :jira-user-name jira-user-name))))))

(def app
  (->
    (wrap-handler multi-handler)
    (wrap-user)
    (wrap-json-body {:keywords? true})
    wrap-json-response
    (wrap-resource "public")))

(def server
  (run-jetty #'app {:port  3000
                    :join? false}))

(comment
  (.stop server)
  )
