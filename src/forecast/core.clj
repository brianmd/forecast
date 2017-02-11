(ns forecast.core
  (:require [forecast.parse :as parse]
            [forecast.repository.ip-locator :refer [use-ipinfo-service]]
            [forecast.repository.location-forecast :refer [use-openweather-service]]
            )
  (:gen-class))

(defn display-usage-message []
  (println "lein run log-filename [num-bins :default 5 [--live]]"))

(defn use-live []
  (println "using live services")
  (use-ipinfo-service)
  (use-openweather-service))

(defn run-local [args]
  (let [filename (first args)
        num-bins (if (< (count args) 2)
                   5
                   (read-string (second args)))
        live? (and (< 2 (count args)) (= "--live" (nth args 2)))]
    (when live? (use-live))
    (parse/run filename num-bins)))

(defn -main
  [& args]
  (if (empty? args)
    (display-usage-message)
    (run-local args)))
