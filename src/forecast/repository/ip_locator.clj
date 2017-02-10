(ns forecast.repository.ip-locator
  (:require [forecast.helpers :refer [valid-ip? log-error]]
            [forecast.repository.storage.memory :as memory]

            [forecast.repository.locate-service.ipinfo-io :as ipinfo-io]
            [forecast.repository.locate-service.random :as random]
            ))

(defonce storage-fns (atom {}))
(defonce locate-service (atom nil))

(defn use-memory-storage []
  (reset! storage-fns {:get #'memory/get-ip
                       :put #'memory/put-ip
                       :clear #'memory/clear-ips
                       :all-locations #'memory/all-locations}))

(defn use-random-service []
  (reset! locate-service #'random/get-location))

(defn use-ipinfo-service []
  (reset! locate-service #'ipinfo-io/get-location))

(defn all-locations []
  ((:all-locations @storage-fns)))

(defn clear-storage []
  ((:clear @storage-fns)))

(defn ip->location
  [ip]
  (try
    (if (valid-ip? ip)
      (if-let [location ((:get @storage-fns) ip)]
        location
        (let [location (@locate-service ip)
              with-timestamp (merge location {:retrieved-on (java.util.Date.)})]
          ((:put @storage-fns) ip with-timestamp)
          with-timestamp))
      (log-error {:error (str "ill-formed ip address: " ip)}))
    (catch Throwable e
      (println e)
      )))

;; alias to a more repository-like command
(def get-location ip->location)

;; (clear-storage)
;; (use-ipinfo-service)
;; (ip->location "8.8.8.8")

;; set defaults
(use-memory-storage)
(use-random-service)
