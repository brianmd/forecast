(ns forecast.repository.storage.datascript
  (:refer-clojure :exclude [find])
  (:require [forecast.metrics :refer [bump]]
            [datascript.core :as d]
            [forecast.helpers :as h]
            ))

(defn ->key
  [keyname]
  (h/encode-as-str keyname))

(defn <-key
  [o]
  (h/decode-str o))

(defn v->hash [v]
  (into {} (map (comp vec rest) v)))

(defn entity->hash [repo entity-id]
  (into {}
        (d/q '[:find ?attr ?value
               :in $ ?entity-id
               :where [?entity-id ?attr ?value]]
             @repo
             entity-id)))

(defn merge-entities [v]
  (let [entity-ids (set (map first v))
        vectors (map (fn [id] (filter #(= id (first %)) v))
                  entity-ids)]
    (->
     (map v->hash vectors)
     )))


(defn find-aux
  "finds record with id 'key'"
  [repo key]
  (dissoc (entity->hash repo key) :gid))

(defn find
  "finds record with id 'key'"
  [repo key]
  (find-aux repo [:gid (->key key)]))

(defn query
  [repo key-value]
  (let [key (ffirst key-value)
        value (second (first key-value))]
    (->> (d/q '[:find ?e ?a ?v
                :in $ ?key ?value
                :where
                [?e ?key ?value]
                [?e ?a ?v]]
              @repo
              key value)
         merge-entities
         (map #(vector (<-key (:gid %)) (dissoc % :gid)))
         (into {})
         )))

(defn upsert-cols!
  "set key's value to map 'm'. retains keys not provided in m"
  [repo key m]
  (d/transact! repo [(assoc m :gid (->key key))])
  {key m})

(defn build-repository
  [table-name]
  (let [schema {:gid {:db/unique :db.unique/identity}}
        repo (d/create-conn schema)
        metrics (atom {})]
    {:type            :datascript
     :repo            repo
     :metrics         metrics
     :close!          (fn [& _] (reset! repo {}))
     :find            (partial #'find repo)
     ;; :find-all        #(find-all repo)
     :query           (partial #'query repo)
     :find-seq        identity
     :find-all-seq    identity
     :insert!         identity
     :update-replace! identity
     :update-cols!    (partial #'upsert-cols! repo)
     :upsert-replace! identity
     :upsert-cols!    (partial #'upsert-cols! repo)
     :delete!         identity
     :delete-all!     identity
     }))


;; (def r (build-repository "ip"))
;; (def r2 (build-repository "location"))
;; (forecast.repository.repository/upsert-cols! r "8.8.8.8" {:a 6 :g 6})
;; (forecast.repository.repository/upsert-cols! r "8.8.8.9" {:a 99 :g 6})
;; (forecast.repository.repository/find r "8.8.8.8")
;; (forecast.repository.repository/find r "8.8.8.9")
;; (forecast.repository.repository/query r {:g 6})
;; (upsert-cols! (:repo r) "8.8.8.9" {:g 7})

(def r (build-repository "ip"))
(forecast.repository.repository/upsert-cols! r {:lat 2} {:a 7 :g 6})
(forecast.repository.repository/find r {:lat 2})
(forecast.repository.repository/query r {:g 6})
