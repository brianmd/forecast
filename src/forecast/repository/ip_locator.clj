(ns forecast.repository.ip-locator
  (:require [forecast.repository.locate-service.ipinfo-io :as ipinfo]
            [forecast.repository.storage.memory :as memory]))

(defonce storage (atom {:get memory/get-ip :put memory/put-ip}))

(defn ip->location
  [ip]
  (if-let [location ((:get @storage) ip)]
    location
    (let [location (ipinfo/get-location ip)
          with-timestamp (merge location {:retrieved-on (java.util.Date.)})]
      ((:put @storage) ip with-timestamp)
      with-timestamp)))

;; alias to a more repository-like command
(def get-location ip->location)

;; (clear-ips)
;; (prn (get-location "8.8.8.8"))
