(ns forecast.repository.storage.channel
  (:require [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]
            ))

(def ip-chan (chan))
(def location-chan (chan))

(defn put-ip
  [ip]
  (>! ip-chan ip))

(defn get-ip
  []
  (<! ip-chan))

(defn put-location
  [location]
  (>! location-chan location))

(defn get-location
  []
  (<! location-chan))

