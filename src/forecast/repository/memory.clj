(ns forecast.repository.memory)

(defonce locations (atom {}))
(defn clear-locations [] (reset! locations {}))

(defonce ips (atom {}))
(defn clear-ips [] (reset! ips {}))

(defn put-ip
  [ip location]
  (swap! ips assoc ip location))

(defn get-ip
  [ip]
  (@ips ip))

(defn put-location
  [location forecast]
  (swap! locations assoc location forecast))

(defn get-location
  [location]
  (@locations location))
