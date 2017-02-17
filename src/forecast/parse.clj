(ns forecast.parse
  (:require [clojure.string :as string]
            [clojure.java.io]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [go]]

            [forecast.helpers :as h]
            [forecast.metrics :as metrics]
            [forecast.repository.ip-locator :as ip]
            [forecast.repository.location-forecast :as location]
            [forecast.repository.storage.memory :as memory]
            ))

(defn use-memory-storage
  []
  (ip/use-memory-storage)
  (location/use-memory-storage))

(defn use-aerospike-storage
  []
  (ip/use-aerospike-storage)
  (location/use-aerospike-storage))

(defn parse-logfile
  [filename f]
  (with-open [rdr (clojure.java.io/reader (str "data/" filename))]
    (doseq [line (line-seq rdr)]
      (h/bump :num-logs)
      (f line))))

(defn log-parser
  [line]
  (->
   line
   (string/split #"\t")
   (nth 23)          ;; ip address
   ip/store-ip
   ))

(defn process-new-ips
  []
  (doseq [ip (ip/new-ips)]
    (ip/find-location ip)))

(defn process-new-locations
  []
  (doseq [loc (location/new-locations)]
    (location/find-forecast loc)))

(def stop-threads? (atom false))

(defn infinite-loop
  ([f] (infinite-loop f nil))
  ([f delay]
   (go
     (loop []
       (f)
       (if delay (Thread/sleep delay))
       (when-not @stop-threads? (recur))
       ))))

(defn sleep-forever []
  (loop []                           ;; prevent program from closing
    (Thread/sleep 1e9)
    (recur)))

(defn daemon
  []
  (infinite-loop process-new-ips 1000)
  (infinite-loop process-new-locations 1000)
  (sleep-forever))

(defn get-histogram
  [num-bins]
  (let [temps (location/done-temperatures)]
    (if (empty? temps)
      []
      (h/histogram temps num-bins))))

(defn print-histogram
  [num-bins]
  (let [bins (get-histogram num-bins)]
    (if bins
      (do
        (println "\nbucketMin\tbucketMax\tcount")
        (doseq [bin bins]
          (println (clojure.string/join "\t" [(h/round-digits 1 (first bin)) (h/round-digits 1 (second bin)) (int (nth bin 2))]))))
      (println "no temperatures found"))
    )
  )

(defn use-live []
  (ip/use-ipinfo-service)
  (location/use-openweather-service))

(defn parse-args
  [params]
  (metrics/reset-metrics)
  (let [args (set params)
        process? (contains? args "--process")
        num-bins-location (if process? second first)]
    ;; setup
    (when (contains? args "--aero")
      (println "using aerospike")
      (use-aerospike-storage))
    (when (contains? args "--live")
      (println "using live services")
      (use-live))

    ;; load logfile
    (when (and process? (not (= \- (-> params first first))))
      (println "load file " (first params))
      (parse-logfile (first params) log-parser))

    ;; post processing
    (when process?
      (println "processing ...")
      (process-new-ips)
      (process-new-locations)
      )
    (when (contains? args "--daemon")
      (println "setting up a daemon")
      (daemon))
    (when (contains? args "--hist")
      (let [num-bins (if (= \- (-> params num-bins-location first))
                       5
                       (read-string (num-bins-location params)))]
        (print-histogram num-bins)))
    ))


;; (parse-logfile "logfile" log-parser)
;; (parse-logfile "logfile-big" log-parser)

;; (process-new-locations)
;; (process-all-locations)
;; (ip/new-ips)
;; (location/new-locations)
;; (location/done-temperatures)
;; (:close! @ip/ip-repo)

;; (process-new-locations)
;; (location/done-temperatures)
;; (get-histogram 5)


;; (use-memory-storage)
;; (use-aerospike-storage)
;; (run "logfile" 5)


