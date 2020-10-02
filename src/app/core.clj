(ns app.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :as bidi]
            [clojure.pprint :as pp]
            [clojure.data.json :as json]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [app.routes :refer [routes multi-handler]]
            [app.controllers.login :as login]
            [app.controllers.project :as project]
            [app.controllers.ping :as ping]
            [raven-clj.core :refer [capture]]
            [raven-clj.ring :refer [wrap-sentry]]
            ))

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
      (time (handler request*) ))))

(def app
  (->
    (wrap-handler multi-handler)
    (wrap-json-body {:keywords? true})
    wrap-json-response
    (wrap-resource "public")
    ))

(def server
  (run-jetty #'app {:port  3000
                    :join? false}))

(comment
  (.getHost (.getContext server))
  (defn ok [arg] (prn arg) {:status 200
                            :body   "123"})
  (get {"oauth_verifier" "D0eyQx"} "oauth_verifier")
  (oauth-callback {:query-params {"oauth_verifier" "D0eyQx"}})
  (t/date-time 1986 10 14)
  (get {:test 123} "test")
  (.stop server)
  (repo/load-users))
