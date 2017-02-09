(ns forecast.repository.ip-locator
  (:require [forecast.helpers :refer [valid-ip?]]
            [forecast.repository.storage.memory :as memory]

            [forecast.repository.locate-service.ipinfo-io :as ipinfo-io]
            [forecast.repository.locate-service.random :as random]
            ))

(defonce storage (atom {}))
(defonce locate-service (atom nil))

(defn use-memory-storage []
  (reset! storage {:get memory/get-ip
                   :put memory/put-ip
                   :all-locations memory/all-locations}))

(defn use-random-service []
  (reset! locate-service random/get-location))

(defn use-ipinfo-service []
  (reset! locate-service ipinfo-io/get-location))

(defn all-locations []
  ((:all-locations @storage)))

(defn ip->location
  [ip]
  (if (valid-ip? ip)
    (if-let [location ((:get @storage) ip)]
      location
      (let [location (@locate-service ip)
            with-timestamp (merge location {:retrieved-on (java.util.Date.)})]
        ((:put @storage) ip with-timestamp)
        with-timestamp))
    {:error (str "ill-formed ip address: " ip)}))

;; alias to a more repository-like command
(def get-location ip->location)

;; (clear-ips)
;; (prn (get-location "8.8.8.8"))

;; set defaults
(use-memory-storage)
(use-random-service)
