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
;; (doseq [loc (location/new-locations)] (println loc))

(defn infinite-loop [f]
  (f)
  (future (infinite-loop f))
  nil)

(defn daemon
  []
  (.start
   (Thread.
    (infinite-loop
     #(do
        (Thread/sleep 1000)
        (println "\n\n...................... process-new-ips")
        (process-new-ips)
        ))))
  (.start
   (Thread.
    (infinite-loop
     #(do
        (Thread/sleep 1000)
        (println "\n\n\n\n...................... process-new-locations\n\n\n")
        (process-new-locations)
        ))))
  )

(defn get-histogram
  [num-bins]
  (let [temps (location/done-temperatures)]
    (if (empty? temps)
      []
      ;; (h/histogram (map :temp temps) num-bins))))
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

;; (defn run
;;   [filename num-bins]
;;   (println "\n\n-------------")
;;   (metrics/reset-metrics)
;;   ;; (memory/clear)
;;   (parse-logfile filename log-parser)
;;   (process-new-ips)
;;   (process-new-locations)
;;   ;; (metrics/print-metrics)
;;   ;; (println "ip metrics:" (:metrics @ip/ip-repo))
;;   ;; (println "location metrics: " (:metrics @location/location-repo))
;;   (print-histogram)
;;   )

(defn use-live []
  (ip/use-ipinfo-service)
  (location/use-openweather-service))

(defn parse-args
  [params]
  (metrics/reset-metrics)
  (let [args (set params)]
    ;; setup
    (when (contains? args "--aero")
      (println "using aerospike")
      (use-aerospike-storage))
    (when (contains? args "--live")
      (println "using live services")
      (use-live))

    ;; process file
    (when-not (and (first params) (= \- (-> params first first)))
      (println "load file")
      (parse-logfile (first params) log-parser))

    ;; post processing
    (when (contains? args "--process")
      (println "processing ...")
      (process-new-ips)
      (process-new-locations)
      )
    (when (contains? args "--daemon")
      (println "setting up a daemon")
      (daemon))
    (when (contains? args "--hist")
      (let [num-bins (if (= \- (-> params second first))
                       5
                       (read-string (second params)))]
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


