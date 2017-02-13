(ns forecast.parse
  (:require [clojure.string :refer [split]]
            [clojure.java.io]
            [clojure.tools.logging :as log]

            [forecast.helpers :as h]
            [forecast.metrics :as metrics]
            [forecast.repository.ip-locator :as ip]
            [forecast.repository.location-forecast :as location]
            [forecast.repository.storage.memory :as memory]
            ))

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
   (split #"\t")
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
;; (doseq [loc (location/new-locations)] (println loc))

(defn get-histogram
  [num-bins]
  (let [temps (location/done-temperatures)]
    (if (empty? temps)
      []
      ;; (h/histogram (map :temp temps) num-bins))))
      (h/histogram temps num-bins))))

(defn run
  [filename num-bins]
  (println "\n\n-------------")
  (metrics/reset-metrics)
  ;; (memory/clear)
  (parse-logfile filename log-parser)
  (process-new-ips)
  (process-new-locations)
  (metrics/print-metrics)
  (println "ip metrics:" (:metrics @ip/ip-repo))
  (println "location metrics: " (:metrics @location/location-repo))
  (let [bins (get-histogram num-bins)]
    (if bins
      (do
        (println "\nbucketMin\tbucketMax\tcount")
        (doseq [bin bins]
          (println (clojure.string/join "\t" [(h/round-digits 1 (first bin)) (h/round-digits 1 (second bin)) (int (nth bin 2))]))))
      (println "no temperatures found"))
    )
  ;; metrics/metrics
  )

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


;; (use-aerospike-storage)
;; (run "logfile" 5)

