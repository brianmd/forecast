(ns forecast.repository.storage.memory
  (:refer-clojure :exclude [find])
  (:require [forecast.metrics :refer [bump]]))

(defn find
  [repo key]
  (get @repo key))

(defn find-all
  [repo]
  ;; (into [] @repo)
  (vals @repo)
  )

(defn upsert-cols!
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

