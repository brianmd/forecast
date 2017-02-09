(ns forecast.repository.ip-locator
  (:require [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]
            [clojure.string :refer [split]]
            ))

(defonce ips (atom {}))
(defn clear-ips [] (reset! ips {}))

(defn force-ip->location
  [ip]
  (try
    (let [url (str "http://ipinfo.io/" ip)
          response (client/get url {:accept :json :socket-timeout 1000 :conn-timeout 1000})]
      (if (= (:status response) 200)
        (->
         ((parse-string (:body response)) "loc")
         (split #",")
         ((fn [v] {:latitude (read-string (first v)) :longitude (read-string (second v))}))
         )
        {:error (str "error response for ip (" (:status response) ")")}
        ))
    (catch Exception e {:error (str e)})))

(defn ip->location
  [ip]
  (if-let [location (@ips ip)]
    location
    (let [location (force-ip->location ip)]
      (swap! ips assoc ip location)
      location)))

;; (clear-ips)
;; (prn (ip->location "8.8.8.8"))
