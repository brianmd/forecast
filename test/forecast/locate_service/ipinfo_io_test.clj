(ns forecast.locate-service.ipinfo-io-test
  (:require [clojure.test :refer :all]
            [forecast.helpers :as h]
            [forecast.repository.locate-service.ipinfo-io :as ip]
            ))

(deftest find-location-test
  (testing "service"
    (let [location (ip/find-location "8.8.8.8")]
      (is (= h/lat-long-8.8.8.8 location))
      )))
