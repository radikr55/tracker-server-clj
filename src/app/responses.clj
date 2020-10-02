(ns app.responses)

(defn ok [response]
  {:status  200
   :headers {"content-type" "application/json"}
   :body    response})

(defn error [response ex]
  (print ex)
  {:status 404
   :body   response})
