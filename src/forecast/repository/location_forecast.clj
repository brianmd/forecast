(ns forecast.repository.location-forecast
  (:require [forecast.repository.storage.memory :as memory]
            [forecast.repository.forecast-service.openweathermap-org :as openweather]
            [forecast.repository.forecast-service.random :as random]
   ))

(defonce storage (atom {}))
(defonce forecast-service (atom nil))

(defn use-memory-storage []
  (reset! storage {:get memory/get-location
                   :put memory/put-location
                   :all-temperatures memory/all-temperatures}))

(defn use-random-service []
  (reset! forecast-service random/get-forecast))

(defn use-openweather-service []
  (reset! forecast-service openweather/get-forecast))

(defn all-temperatures []
  ((:all-temperatures @storage)))

(defn location->forecast
  [location]
  ;; TODO: could validate location before making this call
  (if-let [forecast ((:get @storage) location)]
    forecast
    (let [forecast (@forecast-service location)]
      ((:put @storage) location forecast)
      forecast)))

;; alias to a more repository-like command
(def get-forecast location->forecast)

;; (clear-locations)
;; (prn (get-forecast {:latitude 37.386 :longitude -122.0838}))

;; set defaults
(use-memory-storage)
(use-random-service)
