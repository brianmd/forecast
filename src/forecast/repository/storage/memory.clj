(ns forecast.repository.storage.memory
  (:refer-clojure :exclude [find])
  (:require [forecast.metrics :refer [bump]]))

(defn find
  "finds record with id 'key'"
  [repo key]
  (get @repo key))

(defn find-all
  [repo]
  (vals @repo))

(defn query
  [repo key-val]
  (let [key (first (first key-val))
        val (second (first key-val))]
    (into {}
          (filter (fn [[k v]] (= val (get v key))) @repo))
    ))
(comment
  (query (atom {:x {:a 4 :b 5}
                :y {:a 7}
                :z {:a 4 :c 9}})
         {:a 4})
  )

(defn upsert-cols!
  "set key's value to map 'm'. retains keys not provided in m"
  [repo key m]
  (if (contains? @repo key)
    (swap! repo update key #(merge % m))
    (swap! repo assoc key m)))

(defn build-repository
  [table-name]
  (let [repo (atom {})
        metrics (atom {})]
    {:repo            repo
     :metrics         metrics
     :find            (partial find repo)
     :find-all        #(find-all repo)
     :query           (partial query repo)
     :find-seq        identity
     :find-all-seq    identity
     :insert!         identity
     :update-replace! identity
     :update-cols!    (partial upsert-cols! repo)
     :upsert-replace! identity
     :upsert-cols!    (partial upsert-cols! repo)
     :delete!         identity
     :delete-all!     identity
     }))

