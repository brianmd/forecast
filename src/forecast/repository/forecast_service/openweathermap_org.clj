(ns forecast.repository.forecast-service.openweathermap-org
  (:require [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]
            [clojure.walk :refer [keywordize-keys]]
            )
  (:import [java.util Calendar]))

(defn api
  []
  (or (System/getenv "WEATHER_API") (throw "Must set WEATHER_API envrionment variable. Sign up at http://www.openweathermap.com/.")))

(defn tomorrow
  "return tomorrow's date as a string in yyyy-mm-dd format"
  []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd")
           (java.util.Date. (+ (* 1 86400 1000) (.getTime (java.util.Date.))))
           ))

(defn get-forecast
  [location]
  (try
    (let [url (str "http://api.openweathermap.org/data/2.5/forecast?units=imperial&lat=" (:latitude location) "&lon=" (:longitude location) "&APPID=" (api))
          response (client/get url {:accept :json :socket-timeout 1000 :conn-timeout 1000})
          day (re-pattern (str (tomorrow) ".*"))
          ]
      (if (= (:status response) 200)
        (->>
         (keywordize-keys (parse-string (:body response)))
         :list
         (filter #(re-matches day (:dt_txt %)))
         (map #(-> % :main :temp_max))
         (apply max)
         )
        {:error (str "error response for location (" (:status response) ", " (:body response) ")")}
        ))
    (catch Exception e {:error (str e)})))

