(ns forecast.core
  (:require [forecast.parse :as parse]
            [forecast.repository.ip-locator :refer [use-ipinfo-service]]
            [forecast.repository.location-forecast :refer [use-openweather-service]]
            )
  (:gen-class))

(defn display-usage-message []
  (println "lein run [log-filename [num-bins :default 5]] [--live] [--aero] [--parse|--daemon] [--hist]"))

(defn -main
  [& args]
  (if (empty? args)
    (display-usage-message)
    (parse/parse-args args)
    ))
