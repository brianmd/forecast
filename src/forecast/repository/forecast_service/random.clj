(ns forecast.repository.forecast-service.random
  (:require [forecast.helpers :refer [bump]]
            ))

(defn find-forecast
  [location]
  (bump [:location :service-finds])
  (+ 25 (rand 100)))
