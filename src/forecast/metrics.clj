(ns forecast.metrics
  (:require [clojure.pprint :refer [pprint]]))

(defn- service-metric-builder []
  {
   ;; :service-finds 0
   ;; :inserts       0
   ;; :finds         0

   ;; :num-gets    0
   ;; :num-finds   0
   ;; :num-inserts 0
   })

(defn- metrics-builder []
  {
   :num-logs 0
   :ip       (service-metric-builder)
   :location (service-metric-builder)
   })

(def metrics (atom (metrics-builder)))

(defn print-metrics []
  (pprint @metrics))

(defn reset-metrics []
  (reset! metrics (metrics-builder)))

(defn bump
  ([metric] (bump metrics metric))
  ([metrics metric]
   (try
     (let [k (if (vector? metric) metric (vector metric))
           v (get-in @metrics k)]
       (if v
         (swap! metrics update-in k inc)
         (swap! metrics assoc-in k 1)
         ))
     (catch Throwable e
       (println "error in bump. no such metric: " metric metrics)))))

