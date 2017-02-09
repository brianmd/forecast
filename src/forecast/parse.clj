(ns forecast.parse
  (:require [clojure.string :refer [split]]
            [clojure.java.io]

            [forecast.repository.ip-locator :refer [get-location all-locations]]
            [forecast.repository.location-forecast :refer [get-forecast all-temperatures]]
            [forecast.helpers :refer [histogram]]
            ))

(defn parse-logfile
  [filename f]
  (println "\n----------- retrieve ip address locations")
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
   println
   ))

(defn process-all-locations
  []
  (doseq [loc (all-locations)]
    (get-forecast loc)))

(defn get-histogram
  []
  (histogram (all-temperatures) 4))

(defn run
  [filename]
  (parse-logfile filename log-parser)
  (process-all-locations)
  (get-histogram))

;; (parse-logfile "logfile" log-parser)
;; (parse-logfile "logfile-big" log-parser)

;; (process-all-locations)
;; (all-temperatures)

;; (get-histogram)

;; (run "logfile")
