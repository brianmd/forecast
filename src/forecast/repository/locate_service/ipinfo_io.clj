(ns forecast.repository.locate-service.ipinfo-io
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :refer [split]]
            [clojure.tools.logging :as log]

            [forecast.helpers :as h]
            ))

(defn find-location
  [ip]
  (h/bump [:ip :service-finds])
  (try
    (let [url (str "http://ipinfo.io/" ip)
          response (client/get url {:accept :json :socket-timeout 1000 :conn-timeout 1000})]
      (if (= (:status response) 200)
        (->
         ((json/parse-string (:body response)) "loc")
         (split #",")
         ((fn [v] {:latitude (read-string (first v)) :longitude (read-string (second v))}))
         )
        (do
          (log/errorf "ill-formed ip (status: %s): %s" (:status response) ip)
          {:error (str "error response for ip (" (:status response) ")")})
        ))
    (catch Throwable e
      (if (re-find #"(?m)^.*status 429.*$" (str e))
        (log/errorf "too many requests to ipinfo-io: %s" ip)
        (log/errorf e "error in ipinfo-io/find-location eee: %s" ip))
      {:error (str e)})))


