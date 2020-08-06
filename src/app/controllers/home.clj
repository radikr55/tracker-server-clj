(ns app.controllers.home
  (:require
   [clj-http.client :as client]
   [app.routes :refer [multi-handler]]
   [app.responses :as responses]
   [app.jira.auth :as auth]))


(defmethod multi-handler :home
  [request]
  (throw ( Exception. "test" )))
