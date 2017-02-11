(ns forecast.repository.ip-locator
  (:require [forecast.helpers :refer [valid-ip? log-error bump]]
            [forecast.repository.storage.memory :as memory]

            [forecast.repository.locate-service.ipinfo-io :as ipinfo-io]
            [forecast.repository.locate-service.random :as random]
            ))

(defonce storage-fns (atom {}))
(defonce locate-service (atom nil))

(defn use-memory-storage []
  (reset! storage-fns {:find #'memory/find-ip
                       :insert #'memory/insert-ip
                       :clear #'memory/clear-ips
                       :all-locations #'memory/all-locations}))

(defn use-random-service []
  (reset! locate-service #'random/find-location))

(defn use-ipinfo-service []
  (reset! locate-service #'ipinfo-io/find-location))

(defn all-locations []
  ((:all-locations @storage-fns)))

(defn clear-storage []
  ((:clear @storage-fns)))

(defn find-storage [ip]
  ((:find @storage-fns) ip))

(defn insert-storage [ip m]
  ((:insert @storage-fns) ip m))

(defn ip->location
  [ip]
  (try
    (if (valid-ip? ip)
      (if-let [location (find-storage ip)]
        location
        (let [location (@locate-service ip)
              with-timestamp (merge location {:retrieved-on (java.util.Date.)})]
          (insert-storage ip with-timestamp)
          with-timestamp))
      (log-error {:error (str "ill-formed ip address: " ip)}))
    (catch Throwable e
      (println e)
      )))

;; alias to a more repository-like command
(def find-location ip->location)

;; (clear-storage)
;; (use-ipinfo-service)
;; (ip->location "8.8.8.8")

;; set defaults
(use-memory-storage)
(use-random-service)
