(ns forecast.helpers
  (:require [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]

            [forecast.metrics :as metrics]
            )
  (:import [org.jfree.data.statistics HistogramDataset]
           [java.net InetAddress]
           ))

(defn log-it [& args]
  (log/info (pr-str args))
  (last args))

(defn valid-ip?
  [ip-addr]
  (try
    (let [bytes (->> (clojure.string/split ip-addr #"\.")
                     (map read-string)
                     byte-array
                     )
          inet-addr (InetAddress/getByAddress bytes)
          ]
      (not
       (or
        (.isAnyLocalAddress inet-addr)
        ;; oddly, "710.9.0.124" becomes a valid ip address of "/198.9.0.124"
        (not (= ip-addr (.getHostAddress inet-addr)))
        (.isLoopbackAddress inet-addr)
        (.isSiteLocalAddress inet-addr)
        )))
    (catch Throwable e
      false)))
;; (map is-valid? ["8.8.8.8" "127.0.0.1" "10.9.0.124" "710.9.0.124"])



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
  (pr-str o))
(defn <-keyname
  [o]
  (read-string o))

(defn ->map
  [rec]
  (if rec (hash->map (.bins (.record rec)))))

(defn add-state
  [_ m]
  (let [; s (->keyname key)
        date (now)
        m (cond-> m ;; (assoc m :id key)
            (not (contains? m :state)) (assoc :state "new"
                                              :stated-on date
                                              :created-on date)
            )
        ]
    m))

(defn encode-as-str
  [o]
  (if (string? o)
    o
    (str "~e" (pr-str o))))

(defn decode-str
  [o]
  (if (and (string? o) (= \~ (first o)) (<= 4 (count o)) (= "~e" (subs o 0 2)))
    (read-string (subs o 2))
    o))

(defn mapm
  "returns PersistentArrayMap with maps applied to f.
  f should return seq of 2 element seqs."
  ([f] (partial mapm f))
  ([f & maps]
   (into {}
         (apply map f maps))))
;; (mapm (fn [[k v]] (vector k(inc v))) {:a 1 :b 5 :c 9})
