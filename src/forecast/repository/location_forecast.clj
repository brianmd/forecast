(ns forecast.repository.location-forecast
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [forecast.repository.repository :as r]

            [forecast.repository.storage.memory :as memory]

            [forecast.repository.forecast-service.openweathermap-org :as openweather]
            [forecast.repository.forecast-service.random :as random]

            [clojure.set :as set]))

(defonce forecast-service (atom nil))
(def location-repo (atom nil))

(defn use-memory-storage []
  (reset! location-repo (memory/build-repository "location")))

(defn use-random-service []
  (reset! forecast-service #'random/find-forecast))

(defn use-openweather-service []
  (reset! forecast-service #'openweather/find-forecast))

(defn find-forecast
  [location]
  ;; TODO: could validate location before making this call
  (try
    (let [key (select-keys location [:latitude :longitude])]
      (if-let [forecast (r/find @location-repo key)]
        forecast
        (let [forecast (@forecast-service key)]
          (r/upsert-cols! @location-repo key {:temp forecast})
          forecast)))
    (catch Throwable e
      (log/errorf e "ill-formed location:  %s" location)
      )))

(defn all-temperatures []
  (r/query @location-repo {"state" "new"})
  ;; (r/find-all @location-repo)
  )

;; set defaults
(use-memory-storage)
(use-random-service)

;; (:repo @location-repo)
