(ns app.config
  (:require [aero.core :refer (read-config)]
            [clojure.java.io :refer [resource]]))

(def config-file (delay (read-config (resource "config/config.edn"))))

(def config @config-file)

