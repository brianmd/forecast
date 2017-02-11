(ns forecast.repository.locate-service.random
  (:require [forecast.helpers :refer [bump]]))

(def lat-range [33.063924198120645 41.64007838467894])
(def long-range [-117.59765625 -79.189453125])

(defn find-location
  [ip]
  (bump [:ip :service-finds])
  (let [lat (+ (first lat-range) (rand (- (apply - lat-range))))
        long (+ (first long-range) (rand (- (apply - long-range))))]
    {:latitude lat :longitude long}))
