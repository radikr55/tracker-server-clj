(ns app.repository.redis
  (:require [taoensso.carmine :as car]
            [app.config :refer [config]]))

(def request-token-prefix "REQUEST:")
(def access-token-prefix "ACCESS:")
(def server1-conn {:pool {} :spec (:redis config)})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(defn add-request-token [request-token]
  (wcar* (car/set (str request-token-prefix (:oauth_token request-token)) request-token)
         (car/expire (:oauth_token request-token) 600)))

(defn get-request-token [oauth-token]
  (wcar* (car/get (str request-token-prefix oauth-token))))

(defn add-access-token [request-token]
  (wcar* (car/set (str access-token-prefix (:oauth_token request-token)) request-token)
         (car/expire (str access-token-prefix (:oauth_token request-token)) 600)))

(defn get-access-token [oauth-token]
  (wcar* (car/get (str access-token-prefix oauth-token))))


(comment
  (wcar* (car/ping) (car/set "123" "test") (car/get "123"))
  (wcar* (car/expire (str request-token-prefix "test") 10))
  (wcar* (car/ttl (str request-token-prefix "test")))
  (:test (wcar* (car/get "123")))
  (add-request-token {:oauth_token "test" })
  (get-request-token "test")
  )
