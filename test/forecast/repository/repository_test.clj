(ns forecast.repository.repository-test
  (:refer-clojure :exclude [find])
  (:require [forecast.repository.repository :refer :all :as repo]
            [forecast.repository.storage.memory :as memory]
            [clojure.test :refer :all]
            ))

(deftest memory-repository-test
  (let [r (forecast.repository.storage.memory/build-repository "ip")]
    (testing "upsert-cols!"
      (upsert-cols! r :x {:f 4})
      (upsert-cols! r :x {:g 4})
      (is (= {:f 4 :g 4} (find r :x)))
      (upsert-cols! r :y {:f 4})
      (is (= {:f 4 :g 4} (find r :x)))

      (is (= #{{:f 4} {:f 4 :g 4}} (set (repo/find-all r))))

      ;; (is (= {:x {:f 4 :g 4} :y {:f 4}} @(:repo r))) ;; going inside black box
      ;; (is (= {:x {:f 4 :g 4}} (memory/query (:repo r) {:g 4})))
      (is (= {:x {:f 4 :g 4}} (repo/query r {:g 4})))
      (is (= {:x {:f 4 :g 4} :y {:f 4}} (repo/query r {:f 4})))
      )))

