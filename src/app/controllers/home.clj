(ns app.controllers.home
  (:require [app.routes :refer [multi-handler]]))

(defmethod multi-handler :home [_]
  (throw ( Exception. "test" )))
