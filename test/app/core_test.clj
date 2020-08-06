(ns app.core-test
  (:require [clojure.test :refer :all]
            [app.router :refer [multi-handler]]
            [app.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= {:status 200 :headers {"content-type" "text/plain"} :body "test"}
           ((wrap-handler multi-handler)
            {:request-method :post
             :uri            "/login?foo=42"})))))
