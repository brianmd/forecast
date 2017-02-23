(ns forecast.repository.ip-locator
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [forecast.helpers :as h :refer [valid-ip? bump now]]
            [forecast.repository.repository :as r]
            [forecast.repository.location-forecast :as forecast]
            ))

(defonce locate-service (atom nil))
(def ip-repo (atom nil))

(defn store-ip
  [ips]
  (doseq [ip (map string/trim (string/split ips #","))]
    (r/upsert-cols! @ip-repo ip (h/add-state ip {}))))

(defn find-location
  [ip]
  (try
    (if (valid-ip? ip)
      (let [location (r/find @ip-repo ip)]
        (if (and location (:latitude location))
          location
          (let [_ (r/upsert-cols! @ip-repo ip {:state "processing"})
                lat-long (@locate-service ip)]
            (if (and (map? lat-long) (contains? lat-long :error))
              (r/upsert-cols! @ip-repo ip (merge {:state "error" :retrieved (now)} lat-long))
              (do
                (r/upsert-cols! @ip-repo ip (merge {:state "done" :retrieved (now)} lat-long))
                (forecast/store-location lat-long)
                ))
            lat-long)))
      (do
        (log/errorf "ill-formed or local ip address: %s" ip)
        (r/upsert-cols! @ip-repo ip {:state "error"})
        {:error (str "ill-formed ip address: " ip)})
      )
    (catch Throwable e
      (r/upsert-cols! @ip-repo ip {:state "error"})
      (log/errorf e "ill-formed or local ip address: %s" ip)
      )))

(defn new-ips []
  ;; (map (comp :id second) (r/query @ip-repo {:state "new"})))
  ;; (map first (r/query @ip-repo {:state "new"})))
  (keys (r/query @ip-repo {:state "new"})))

;; (use-aerospike-storage)
;; (new-ips)
;; (r/query @ip-repo {:state "new"})
;; (r/find @ip-repo "10.8.0.1")
;; (forecast.repository.storage.aerospike/query (:repo @ip-repo) "ip" {:state "new"})

;; (forecast.repository.storage.aerospike/query (:repo @forecast.repository.ip-locator/ip-repo) "ip" {:state "new"})
;; (def x (forecast.repository.storage.aerospike/query (:repo @forecast.repository.ip-locator/ip-repo) "ip" {:state "new"}))
;; (-> (first x) decode-bin-map)

;; (forecast.repository.storage.aerospike/query (:repo @forecast.repository.location-forecast/location-repo) "location" {:state "new"})
;; (forecast.repository.storage.aerospike/upsert-cols! (:repo @forecast.repository.location-forecast/location-repo) "location" {:lat-lon 4} {:state "hmm"})
