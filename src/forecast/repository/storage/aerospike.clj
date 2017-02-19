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

(def nspace "test")

(defn ->key
  [keyname]
  (h/encode-as-str keyname))

(defn <-key
  [o]
  (h/decode-str o))

(defn ->bin-key
  [keyname]
  (h/encode-as-str keyname))

(defn <-bin-key
  [o]
  (h/decode-str o))

(defn ->bin-value
  [o]
  (cond
    (number? o) o
    :else (h/encode-as-str o)))

(defn <-bin-value
  [o]
  (h/decode-str o))

(defn encode-bin-map
  [m]
  (h/mapm (fn [[k v]] [(->bin-key k) (->bin-value v)]) m))

(defn decode-bin-map
  [m]
  (h/mapm (fn [[k v]] [(<-bin-key k) (<-bin-value v)]) m))

(defn setup!
  [init-commands-fn]
  (when-not @aero/conn-atom
    (let [host (or (System/getenv "AEROSPIKE_HOST") "127.0.0.1")
          port (or (System/getenv "AEROSPIKE_PORT") 3000)
          _ (log/info (str "connecting to " host ":" port " ..."))
          repo (aero/connect! host port)]
      (reset! aero/conn-atom repo)))
  (aero/init-once! @aero/conn-atom nspace "test-set")
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
  (let [id (->key key)]
    (aero/put! repo nspace set id
               (encode-bin-map (assoc m :_id id)))
    ))

(defn find
  "finds record with id 'key'"
  [repo set key]
  (let [k (->key key)]
    (let [m (aero/get repo nspace set k)]
      (dissoc (decode-bin-map m) :_id)
      )))

(defn query
  [repo set query-map]
  (let [key-value (first query-map)
        keyname (->bin-key (first key-value))
        value (->bin-value (second key-value))
        recs  (q/query repo
                       (q/mk-statement
                        {:ns nspace :set set}
                        (q/f-equal keyname value)))
        ]
    (let [recs (map #(.bins (.record %)) recs)
          recs (map decode-bin-map recs)
          recs (h/mapm (fn [m] [(:_id m) (dissoc m :_id)]) recs)
          ]
      recs)
    ))

(defn forecast-initial-commands
  [repo]
  (q/create-index! repo nspace "ip"       "ipstate"       (->bin-key :state) :string)
  (q/create-index! repo nspace "location" "locationstate" (->bin-key :state) :string)
  )

(defn build-repository
  [set-name]
  (let [repo (setup! #'forecast-initial-commands)
        metrics (atom {})]
    {:type            :aerospike
     :repo            repo
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
