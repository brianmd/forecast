(ns forecast.repository.location-forecast
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [forecast.helpers :as h]
            [forecast.repository.repository :as r]

            [forecast.repository.storage.memory :as memory]
            [forecast.repository.storage.aerospike :as aero]

            [forecast.repository.forecast-service.openweathermap-org :as openweather]
            [forecast.repository.forecast-service.random :as random]

            [clojure.set :as set]))

(defonce forecast-service (atom nil))
(def location-repo (atom nil))

(defn use-memory-storage []
  (reset! location-repo (memory/build-repository "location")))

(defn use-aerospike-storage []
  (reset! location-repo (aero/build-repository "location")))

(defn use-random-service []
  (reset! forecast-service #'random/find-forecast))

(defn use-openweather-service []
  (reset! forecast-service #'openweather/find-forecast))

(defn store-location
  [lat-long]
  (r/upsert-cols! @location-repo lat-long (h/add-state lat-long {})))

(defn find-forecast
  [location]
  ;; TODO: could validate location before making this call
  (let [lat-long (select-keys location [:latitude :longitude])
        key lat-long]
    (try
      (do
        (r/upsert-cols! @location-repo key {:state "processing"})
        (let [forecast (r/find @location-repo key)]
          (if (and forecast (:temp forecast))
            (do
              (r/upsert-cols! @location-repo key (assoc forecast :state "done"))
              forecast)
            (let [forecast (@forecast-service key)]
              (r/upsert-cols! @location-repo key {:temp forecast :state "done"})
              forecast))))
      (catch Throwable e
        (log/errorf e "ill-formed location:  %s" location)
        (r/upsert-cols! @location-repo key {:state "error"})
        ))))

(defn new-maps []
  (r/query @location-repo {:state "new"}))
(defn done-maps []
  (r/query @location-repo {:state "done"}))

(defn new-locations []
  (keys (r/query @location-repo {:state "new"})))

(defn done-temperatures []
  (map :temp (vals (r/query @location-repo {:state "done"}))))

;; set defaults
(use-memory-storage)
(use-random-service)
