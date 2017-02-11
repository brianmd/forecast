(ns forecast.repository.storage.memory
  (:require [forecast.metrics :refer [bump]]))

(defonce ips (atom {}))
(defn clear-ips [] (reset! ips {}))

(defonce locations (atom {}))
(defn clear-locations [] (reset! locations {}))

(defn clear
  []
  (clear-ips)
  (clear-locations))

(defn insert-ip
  [ip location]
  (bump [:ip :inserts])
  (swap! ips assoc ip location))

(defn find-ip
  [ip]
  (bump [:ip :finds])
  (@ips ip))

(defn all-locations
  []
  (vals @ips))

(defn insert-location
  [location forecast]
  (bump [:location :inserts])
  (swap! locations assoc location forecast))

(defn find-location
  [location]
  (bump [:location :finds])
  (@locations location))

(defn all-temperatures
  []
  (vals @locations))

;; (clear)
