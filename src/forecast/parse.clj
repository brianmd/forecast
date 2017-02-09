(ns forecast.parse
  (:require [clojure.string :refer [split]]
            [clojure.java.io]

            [forecast.repository.ip-locator :refer [get-location all-locations]]
            [forecast.repository.location-forecast :refer [get-forecast all-temperatures]]
            [forecast.helpers :refer [histogram round-digits]]
            ))

(defn parse-logfile
  [filename f]
  (println "\n----------- retrieve ip address locations: " filename)
  (with-open [rdr (clojure.java.io/reader filename)]
    (doseq [line (line-seq rdr)]
      (f line))))

(defn log-parser
  [line]
  (->
   line
   (split #"\t")
   (nth 23)          ;; ip address
   get-location
   ))

(defn process-all-locations
  []
  (doseq [loc (all-locations)]
    (get-forecast loc)))

(defn get-histogram
  [num-bins]
  (histogram (all-temperatures) num-bins))

(defn run
  [filename num-bins]
  (parse-logfile filename log-parser)
  (process-all-locations)
  (let [bins (get-histogram num-bins)]
    (println "bucketMin\tbucketMax\tcount")
    (doseq [bin bins]
      (println (clojure.string/join "\t" [(round-digits 1 (first bin)) (round-digits 1 (second bin)) (int (nth bin 2))])))
    ))

;; (parse-logfile "logfile" log-parser)
;; (parse-logfile "logfile-big" log-parser)

;; (process-all-locations)
;; (all-temperatures)

;; (get-histogram)

;; (run "logfile" 5)
