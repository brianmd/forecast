(ns forecast.repository.ip-locator
  (:require [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]
            [clojure.string :refer [split]]
            ))

(defn ip->location
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
        response
        ))
    (catch Exception e {:error (str e)})))

;; (prn (ip->location "8.8.8.8"))
