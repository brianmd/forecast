(ns forecast.repository.ip-locator
  (:require [clojure.tools.logging :as log]
            [forecast.helpers :as h :refer [valid-ip? bump now]]
            [forecast.repository.repository :as r]

            [forecast.repository.storage.memory :as memory]
            [forecast.repository.storage.aerospike :as aero]


            [forecast.repository.locate-service.ipinfo-io :as ipinfo-io]
            [forecast.repository.locate-service.random :as random]

            [forecast.repository.location-forecast :as forecast]
            ))

(defonce locate-service (atom nil))
(def ip-repo (atom nil))

(defn use-random-service []
  (reset! locate-service #'random/find-location))

(defn use-ipinfo-service []
  (reset! locate-service #'ipinfo-io/find-location))

(defn use-memory-storage []
  (reset! ip-repo (memory/build-repository "ip")))

(defn store-ip
  [ip]
  (r/upsert-cols! @ip-repo ip (h/add-state ip {})))

(defn find-location
  [ip]
  (try
    (if (valid-ip? ip)
      (let [location (r/find @ip-repo ip)]
        (if (and location (:latitude location))
          location
          (let [_ (r/upsert-cols! @ip-repo ip {"state" "processing"})
                lat-long (@locate-service ip)]
            (r/upsert-cols! @ip-repo ip (merge {"state" "done" :retrieved-on (now)} lat-long))
            (forecast/store-location lat-long)
            lat-long)))
      (do
        (log/errorf "ill-formed ip address: %s" ip)
        (r/upsert-cols! @ip-repo ip {"state" "error"})
        {:error (str "ill-formed ip address: " ip)})
      )
    (catch Throwable e
      (r/upsert-cols! @ip-repo ip {"state" "error"})
      (log/errorf e "ill-formed ip address: %s" ip)
      )))

(defn new-ips []
  (map second (r/query @ip-repo {"state" "new"})))

(defn new-locations []
  (map second (r/query @ip-repo {"state" "new"})))

(defn all-locations []
  (r/find-all @ip-repo))

;; set defaults
(use-memory-storage)
(use-random-service)

;; (all-locations)
;; (new-locations)
;; (map second (r/query @ip-repo {"state" "done"}))
;; (:repo @ip-repo)
;; (first @(:repo @ip-repo))
;; ["173.252.110.113" {:id "173.252.110.113", "state" "new", :stated-on 1486948263727, :created-on 1486948263727}]
;; (r/find @ip-repo "173.252.110.113")
;; (find-location "173.252.110.113")
;; (@locate-service "173.252.110.113")
