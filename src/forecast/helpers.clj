(ns forecast.helpers
  (:require [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [forecast.metrics :as metrics]
            )
  (:import [org.jfree.data.statistics HistogramDataset]))

(def ip-regex
  #"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")

(defn valid-ip?
  [ip]
  (not (or (nil? ip) (nil? (re-matches ip-regex ip)))))

(def lat-long-8.8.8.8 {:latitude 37.386 :longitude -122.0838})

(defn bump
  ([metric] (metrics/bump metric))
  ([metrics metric] (metrics/bump metrics metric)))

(defn now
  []
  (System/currentTimeMillis))

(defn print-metrics
  []
  (metrics/print-metrics))

(defn histogram
  [data num-bins]
  (let [hist (HistogramDataset.)]
    (.addSeries hist "x" (double-array data) num-bins)
    (map #(vector (.getStartX hist 0 %) (.getEndX hist 0 %) (.getY hist 0 %)) (range (.getItemCount hist 0)))
    ))
;; (histogram [1 7 22 4 2 19] 5)

(defn round-digits
  "round to specified number of digits"
  ([digits] (fn [num] (round-digits digits num)))
  ([digits num]
   (.divide (bigdec num) 1M digits java.math.RoundingMode/HALF_UP)))

(defn hash->map
  [hashmap]
  (when hashmap
    (keywordize-keys (zipmap (.keySet hashmap) (.values hashmap)))))

(defn ->keyname
  [o]
  (if (string? o) o (str "~edn" (pr-str o))))

(defn ->map
  [rec]
  (if rec (hash->map (.bins (.record rec)))))

(defn add-state
  [key m]
  (let [s (->keyname key)
        date (now)
        m (cond-> (assoc m :id s)
            (not (contains? m :state)) (assoc "state" "new"
                                              :stated-on date
                                              :created-on date)
            )
        ]
    m))
