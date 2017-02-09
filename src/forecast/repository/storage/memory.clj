(ns forecast.repository.storage.memory)

(defonce ips (atom {}))
(defn clear-ips [] (reset! ips {}))

(defonce locations (atom {}))
(defn clear-locations [] (reset! locations {}))

(defn clear
  []
  (clear-ips)
  (clear-locations))

(defn put-ip
  [ip location]
  (swap! ips assoc ip location))

(defn get-ip
  [ip]
  (@ips ip))

(defn all-locations
  []
  (vals @ips))

(defn put-location
  [location forecast]
  (swap! locations assoc location forecast))

(defn get-location
  [location]
  (@locations location))

(defn all-temperatures
  []
  (vals @locations))

;; (clear)
