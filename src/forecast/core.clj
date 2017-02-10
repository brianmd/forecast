(ns forecast.core
  (:require [forecast.parse :as parse]
            [forecast.repository.ip-locator :refer [use-ipinfo-service]]
            [forecast.repository.location-forecast :refer [use-openweather-service]]
            )
  (:gen-class))

(defn display-usage []
  ;; (println "lein run [--use-aerospike] [--use-live-services] log-filename [num-bins :default 5]"))
  (println "lein run log-filename [num-bins :default 5]"))

(defn run [args]
  (let [filename (first args)
        num-bins (if (< (count args) 2)
                   5
                   (read-string (second args)))
        live? (and (< 2 (count args)) (= "--live" (nth args 2)))]
    (when live?
      (println "using live services")
      (use-ipinfo-service)
      (use-openweather-service))
    (parse/run filename num-bins)))

(defn -main
  [& args]
  (if (empty? args)
    (display-usage)
    (run args)))
