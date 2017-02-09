(ns forecast.repository.location-forecast
  (:require [forecast.repository.openweathermap-org :as open-weather]))

(defonce locations (atom {}))
(defn clear-locations [] (reset! locations {}))

(defn location->forecast
  [location]
  (if-let [forecast (@locations location)]
    forecast
    (let [forecast (open-weather/get-forecast location)]
      (swap! locations assoc location forecast)
      forecast)))

;; alias to a more repository-like command
(def get-forecast location->forecast)

;; (clear-locations)
;; (prn (get-forecast {:latitude 37.386 :longitude -122.0838}))
