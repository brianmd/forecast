(ns forecast.repository.location-forecast
  (:require [forecast.repository.forecast-service.openweathermap-org :as open-weather]
            [forecast.repository.storage.memory :as memory]))

(defonce storage (atom {:get memory/get-location :put memory/put-location}))

(defn location->forecast
  [location]
  (if-let [forecast ((:get @storage) location)]
    forecast
    (let [forecast (open-weather/get-forecast location)]
      ((:put @storage) location forecast)
      forecast)))

;; alias to a more repository-like command
(def get-forecast location->forecast)

;; (clear-locations)
;; (prn (get-forecast {:latitude 37.386 :longitude -122.0838}))
