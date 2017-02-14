(ns forecast.repository.repository-test
  (:refer-clojure :exclude [find])
  (:require [forecast.repository.repository :refer :all :as repo]
            [forecast.repository.storage.memory :as memory]
            [forecast.repository.storage.aerospike :as aero]
            [clojure.test :refer :all]
            ))

(deftest memory-repository-test
  (let [r (memory/build-repository "ip")]
    (testing "upsert-cols!"
      (upsert-cols! r :x {:f 4})
      (upsert-cols! r :x {:g 4})
      (is (= {:f 4 :g 4} (find r :x)))
      (upsert-cols! r :y {:f 4})
      (is (= {:f 4 :g 4} (find r :x)))

      (is (= #{{:f 4} {:f 4 :g 4}} (set (repo/find-all r))))

      (is (= {:x {:f 4 :g 4}} (repo/query r {:g 4})))
      (is (= {:x {:f 4 :g 4} :y {:f 4}} (repo/query r {:f 4})))

      (is (= {:x {:f 4 :g 4} :y {:f 4}} @(:repo r))) ;; going inside black box
      (is (= {:x {:f 4 :g 4}} (memory/query (:repo r) {:g 4})))
      )))

(deftest aerospike-repository-test
  (aero/close! nil)
  (let [r (aero/build-repository "ip")]
    (testing "upsert-cols!"
      (upsert-cols! r :x {:state "new"})
      (upsert-cols! r :x {:id "4"})
      (is (= {:state "new" :id "4"} (find r :x)))
      (upsert-cols! r :y {:state "new"})
      (is (= {:state "new" :id "4"} (find r :x)))
      )

      ;; (is (= #{{:f 4} {:f 4 :g 4}} (set (repo/find-all r))))

    (testing "query"
      ;; (is (= {:x {:state "new" :g 4}} (repo/query r {:g 4})))

      (is (= {:x {:state "new" :id "4"} :y {:state "new"}}
             (repo/query r {:state "new"})))
      (is (= {:x {:state "new" :id "4"}}
             (repo/query r {:id "4"})))
      )
    ))

