(ns server-timetracker.core
  (:require [clojure.core :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [clojure.pprint :as p]
            [compojure.core :refer [GET defroutes]]))

(defn app [request]
  (do (p/pprint request)
      {:status  200
       :headers {"content-type" "text/plain"}
       :body    "bo1111dyasd"}))

(def server
  (run-jetty #'app {:port  3000
                    :join? false}))

(defn page-index [request]
  (do (p/pprint request)
      {:status  200
       :headers {"content-type" "text/plain"}
       :body    "basdfody"}))

(defn page-404 [request]
  (do (p/pprint request)
      {:status  404
       :headers {"content-type" "text/plain"}
       :body    "basdfody"}))

(defroutes my-app
  (GET "/"      request (page-index request))
  page-404)

(def app
  (-> my-app
      (wrap-resource "public")))

(comment
  (require '[compojure.core :refer [GET defroutes]])
  (type server)
  (require '[ring.middleware.resource :refer [wrap-resource]])
  (require '[clojure.test :refer :all])
  (require '[clj-time.core :as t])
  (deftest a-test
    (testing "FIXME, I fail."
      (is (= 0 1))))
  (.stop server)
  ()
  (defroutes my-app
    (GET "/"      request (page-index request))
    page-404))
