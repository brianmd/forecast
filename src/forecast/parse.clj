(ns forecast.parse
  (:require [clojure.string :refer [split]]
            [clojure.java.io]

            [forecast.repository.ip-locator :refer [get-location]]
            [forecast.repository.location-forecast :refer [get-forecast]]
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

;; (parse-logfile "logfile" log-parser)

