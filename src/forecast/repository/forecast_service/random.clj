(ns forecast.repository.forecast-service.random)

(defn get-forecast
  [location]
  (+ 25 (rand 50)))
