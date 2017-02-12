(ns forecast.parse
  (:require [clojure.string :refer [split]]
            [clojure.java.io]
            [clojure.tools.logging :as log]

            [forecast.metrics :as metrics :refer [reset-metrics bump]]
            [forecast.repository.ip-locator :refer [find-location all-locations ip-repo]]
            [forecast.repository.location-forecast :refer [find-forecast all-temperatures location-repo]]
            [forecast.repository.storage.memory :as memory]
            [forecast.helpers :refer [histogram round-digits print-metrics]]
            ))

(defn parse-logfile
  [filename f]
  (with-open [rdr (clojure.java.io/reader (str "data/" filename))]
    (doseq [line (line-seq rdr)]
      (bump :num-logs)
      (f line))))

(defn log-parser
  [line]
  (->
   line
   (split #"\t")
   (nth 23)          ;; ip address
   find-location
   ))
;; (parse-logfile "logfile" log-parser)

(defn process-all-locations
  []
  (doseq [loc (all-locations)]
    (find-forecast loc)))

(defn get-histogram
  [num-bins]
  (let [temps (all-temperatures)]
    (if (empty? temps)
      []
      (histogram (map :temp temps) num-bins))))

(defn run
  [filename num-bins]
  (println "\n\n-------------")
  (reset-metrics)
  ;; (memory/clear)
  (parse-logfile filename log-parser)
  (process-all-locations)
  (print-metrics)
  (println "ip metrics:" (:metrics @ip-repo))
  (println "location metrics: " (:metrics @location-repo))
  (let [bins (get-histogram num-bins)]
    (if bins
      (do
        (println "\nbucketMin\tbucketMax\tcount")
        (doseq [bin bins]
          (println (clojure.string/join "\t" [(round-digits 1 (first bin)) (round-digits 1 (second bin)) (int (nth bin 2))]))))
      (println "no temperatures found"))
    )
  ;; metrics/metrics
  )

;; (parse-logfile "logfile" log-parser)
;; (parse-logfile "logfile-big" log-parser)

;; (process-all-locations)
;; (all-temperatures)

;; (get-histogram)

(run "logfile" 5)
