(ns forecast.forecast-service.openweathermap-org-test
  (:require [clojure.test :refer :all]
            [forecast.helpers :as h]
            [forecast.repository.forecast-service.openweathermap-org :as location]
            ))

(deftest find-forecast-test
  (testing "service"
    (let [forecast (location/find-forecast h/lat-long-8.8.8.8)]
      (is (< 0 forecast 110))
      )))

