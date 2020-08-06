(ns app.responses)

(defn ok [response]
  {:status  200
   :headers {"content-type" "application/json"}
   :body    response})

(defn error [response]
  {:status 404
   :body   response})
