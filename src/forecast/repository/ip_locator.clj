(ns forecast.repository.ip-locator
  (:require [forecast.repository.ipinfo-io :as ipinfo]))

(defonce ips (atom {}))
(defn clear-ips [] (reset! ips {}))

(defn ip->location
  [ip]
  (if-let [location (@ips ip)]
    location
    (let [location (ipinfo/get-location ip)
          with-timestamp (merge location {:retrieved-on (java.util.Date.)})]
      (swap! ips assoc ip with-timestamp)
      with-timestamp)))

;; alias to a more repository-like command
(def get-location ip->location)

;; (clear-ips)
;; (prn (get-location "8.8.8.8"))
