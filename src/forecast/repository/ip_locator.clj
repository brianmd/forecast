(ns forecast.repository.ip-locator
  (:require [clojure.tools.logging :as log]
            [forecast.helpers :refer [valid-ip? bump now]]
            [forecast.repository.repository :as r]

            [forecast.repository.storage.memory :as memory]
            [forecast.repository.storage.aerospike :as aero]


            [forecast.repository.locate-service.ipinfo-io :as ipinfo-io]
            [forecast.repository.locate-service.random :as random]
            ))

(defonce locate-service (atom nil))
(def ip-repo (atom nil))

(defn use-random-service []
  (reset! locate-service #'random/find-location))

(defn use-ipinfo-service []
  (reset! locate-service #'ipinfo-io/find-location))

(defn use-memory-storage []
  (reset! ip-repo (memory/build-repository "ip")))

(defn find-location
  [ip]
  (try
    (if (valid-ip? ip)
      (if-let [location (r/find @ip-repo ip)]
        location
        (let [location (@locate-service ip)
              with-timestamp (merge location {:retrieved-on (now)})]
          (r/upsert-cols! @ip-repo ip with-timestamp)
          with-timestamp))
      (do
        (log/errorf "ill-formed ip address: %s" ip)
        {:error (str "ill-formed ip address: " ip)})
      )
    (catch Throwable e
      (log/errorf e "ill-formed ip address: %s" ip)
      )))

(defn all-locations []
  (r/query @ip-repo {"state" "new"})
  ;; (r/find-all @ip-repo)
  )

;; set defaults
(use-memory-storage)
(use-random-service)
