(ns forecast.repository.location-forecast
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [forecast.helpers :as h]
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

(defn store-location
  [lat-long]
  (let [key (h/->keyname lat-long)]
    (r/upsert-cols! @location-repo key (h/add-state key lat-long))))

(defn find-forecast
  [location]
  ;; TODO: could validate location before making this call
  (let [lat-long (select-keys location [:latitude :longitude])
        key (h/->keyname lat-long)]
    (try
      (do
        (r/upsert-cols! @location-repo key {"state" "processing"})
        (let [forecast (r/find @location-repo key)]
          (if (and forecast (:temp forecast))
            forecast
            (let [forecast (@forecast-service key)]
              (r/upsert-cols! @location-repo key {:temp forecast "state" "done"})
              forecast))))
      (catch Throwable e
        (r/upsert-cols! @location-repo key {"state" "error"})
        (log/errorf e "ill-formed location:  %s" location)
        ))))
;; (find-forecast 3)
;; (@forecast-service 3)
;; (h/add-state "xx" {:temp 3})

(defn new-locations []
  (map second (r/query @location-repo {"state" "new"})))

(defn all-temperatures []
  (r/find-all @location-repo))

;; set defaults
(use-memory-storage)
(use-random-service)

;; (store-location {:a 3})
(:repo @location-repo)
