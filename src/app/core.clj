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
            [raven-clj.core :refer [capture]]
            [raven-clj.ring :refer [wrap-sentry]]
            ))

(def atmo (atom ""))
(def DSN "https://3136ce54e4ae4f39adab043bfd49a593:5413c36005bd44daa9cf939f65c4f6cb@o430247.ingest.sentry.io/5378398")

(def dsn "https://b70a31b3510c4cf793964a185cfe1fd0:b7d80b520139450f903720eb7991bf3d@example.com/1")

(capture DSN {:message "Test Exception Message"
              :tags    {:version "1.0"}
              :logger  "main-logger"
              :extra   {:my-key           1
                        :some-other-value "foo bar"}})
;; (let [event (-> nil
;;                 (r/add-exception! (Exception. "test"))
;;                 (r/add-extra! {:somethig "else"}))]
;;   @(r/capture! DSN event)
;;   )      

;; (defn wrap-sentry [handler]
;;   (fn [request]
;;     (try
;;       (handler request)
;;       (catch Exception e
;;         (let [event (-> nil
;;                         (r/add-exception! e)
;;                         (r/add-ring-request! request)
;;                         (r/add-extra! {:somethig "else"}))]
;;           (try @(r/capture! DSN event)
;;                (catch Exception e-sentry
;;                  ;; (print e-sentry "Sentry error: %s" )
;;                  ;; (print e "Request failed")
;;                  ;; (reset! atmo (slurp (:body (ex-data e-sentry)) ))
;;                  ;; (print (:body (ex-data e-sentry) ))
;;                  (print (:data e-sentry))
;;                  )
;;                (finally
;;                  ;; {:status 200
;;                  ;;  :body   @atmo
;;                  ;;  }
;;                  {:status 500
;;                   :body   "Internal error, please try later!"})
;;                ))))))

(defn wrap-handler
  [handler]
  (fn [request]
    (let [{:keys [uri]} request
          request*      (bidi/match-route*
                          routes uri request)]
      ;; (pp/pprint request*)
      (handler request*))))

(def app
  (->
    (wrap-handler multi-handler)
    (wrap-json-body {:keywords? true})
    wrap-json-response
    (wrap-resource "public")
    ;; (wrap-sentry DSN)
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
  (extend-protocol cheshire.generate/JSONable
    org.joda.time.DateTime
    (to-json [dt gen]
      (cheshire.generate/write-string gen (str dt))))
  (.stop server)
  (repo/load-users))
