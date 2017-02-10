(ns forecast.repository.location-forecast
  (:require [forecast.repository.storage.memory :as memory]
            [forecast.repository.forecast-service.openweathermap-org :as openweather]
            [forecast.repository.forecast-service.random :as random]
   ))

(defonce storage-fns (atom {}))
(defonce forecast-service (atom nil))

(defn use-memory-storage []
  (reset! storage-fns {:get              #'memory/get-location
                       :put              #'memory/put-location
                       :clear            #'memory/clear-locations
                       :all-temperatures #'memory/all-temperatures}))

(defn use-random-service []
  (reset! forecast-service #'random/get-forecast))

(defn use-openweather-service []
  (reset! forecast-service #'openweather/get-forecast))

(defn all-temperatures []
  ((:all-temperatures @storage-fns)))

(defn clear-storage []
  ((:clear @storage-fns)))

(defn location->forecast
  [location]
  ;; TODO: could validate location before making this call
  (try
    (if-let [forecast ((:get @storage-fns) location)]
      forecast
      (let [forecast (@forecast-service location)]
        ((:put @storage-fns) location forecast)
        forecast))
    (catch Throwable e
      (println e)
      )))

;; alias to a more repository-like command
(def get-forecast location->forecast)

;; (clear-storage)
;; (use-random-service)
;; (use-openweather-service)
;; (location->forecast {:latitude 37.386 :longitude -122.0838})
;; (map
;;  (fn [n]
;;    (println n)
;;    (clear-storage)
;;    (location->forecast {:latitude 37.386 :longitude -122.0838})
;;    )
;;  (range 10))

;; set defaults
(use-memory-storage)
(use-random-service)
