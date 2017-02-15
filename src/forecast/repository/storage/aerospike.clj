(ns forecast.repository.storage.aerospike
  (:refer-clojure :exclude [find])
  (:require [clojure.walk :refer [stringify-keys keywordize-keys]]
            [clojure.tools.logging :as log]
            [aeroclj.core :as aero]
            [aeroclj.query :as q]
            [forecast.helpers :as h]
            [forecast.metrics :refer [bump]]
            )
  (:import (com.aerospike.client AerospikeClient Key Bin Record Operation Value)
           (com.aerospike.client.query Statement Filter IndexType)
           (com.aerospike.client.policy WritePolicy ClientPolicy GenerationPolicy QueryPolicy RecordExistsAction CommitLevel Policy BatchPolicy)
           (clojure.lang IPersistentMap)
           ))

(defn setup!
  [init-commands-fn]
  (when-not @aero/conn-atom
    (let [_ (log/info "connecting ...")
          host (or (System/getenv "AEROSPIKE_HOST") "192.168.0.213")
          port (or (System/getenv "AEROSPIKE_PORT") 3000)
          repo (aero/connect! host port)]
      (reset! aero/conn-atom repo)))
  (aero/init-once! @aero/conn-atom "test" "test-set")
  (when init-commands-fn (init-commands-fn @aero/conn-atom))
  @aero/conn-atom)
;; (close! nil)

(defn close!
  [_]
  (when @aero/conn-atom
    (try
      (aero/close! @aero/conn-atom)
      (catch Throwable e))
    (reset! aero/conn-atom nil)))

(defn upsert-cols!
  "set key's value to map 'm'. retains keys not provided in m"
  [repo set key m]
  (let [id (h/->keyname key)
        m (stringify-keys m)]
    (aero/put! repo "test" set id
               (assoc m "_id" id))
    ))

(defn find
  "finds record with id 'key'"
  [repo set key]
  (let [k (h/->keyname key)]
    (let [m (aero/get repo "test" set k)]
      ;; convert back to clojure-type keys
      ;; (if m (keywordize-keys (zipmap (.keySet m) (.values m))))
      ;; (if m (keywordize-keys (into {} m)))
      (if m (-> (into {} m) keywordize-keys (dissoc :_id)))
      )))

(defn query
  [repo set key]
  (let [key-value (first key)
        keyname (name (first key-value))
        value (second key-value)
        recs  (q/query repo
                       (q/mk-statement
                        {:ns "test" :set set}
                        ;; {:ns "test" :set set :index "ipid"}
                        (q/f-equal keyname value)))
        ]
    (let [recs (map h/->map recs)]
      (into {} (map #(vector (read-string (:_id %)) (dissoc % :_id))) recs))
    ))
;; (query @aero/conn-atom "ip" {"state" "new"})
;; (-> (query @aero/conn-atom "ip" {"state" "new"}) first second :id)

(defn forecast-initial-commands
  [repo]
  (log/info "ensuring indexes exist ....")
  (q/create-index! repo "test" "ip"       "ipstate"       "state" :string)
  (q/create-index! repo "test" "location" "locationstate" "state" :string)
  )

(defn build-repository
  [set-name]
  (let [repo (setup! forecast-initial-commands)
        metrics (atom {})]
    {:repo            repo
     :metrics         metrics
     :close!          (fn []
                        (close! @repo)
                        (reset! repo nil))
     :find            (partial #'find repo set-name)
     :find-all        identity
     :query           (partial #'query repo set-name)
     :find-seq        identity
     :find-all-seq    identity
     :insert!         identity
     :update-replace! identity
     ;; :update-cols!    (partial upsert-cols! repo set-name)
     :upsert-replace! identity
     :upsert-cols!    (partial upsert-cols! repo set-name)
     :delete!         identity
     :delete-all!     identity
     }))
