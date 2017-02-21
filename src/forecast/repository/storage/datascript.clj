(ns forecast.repository.storage.datascript
  (:refer-clojure :exclude [find])
  (:require [forecast.metrics :refer [bump]]
            [datascript.core :as d]
            ))

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
  (find-aux repo [:gid key]))

(defn query
  [repo key-value]
  (let [key (ffirst key-value)
        value (second (first key-value))]
    (->> (d/q '[:find ?e ?a ?v
                :in $ ?key ?value
                :where
                [?e ?key ?value]
                [?e ?a ?v]]
              @(:repo r)
              key value)
         merge-entities
         (map #(vector (:gid %) (dissoc % :gid)))
         (into {})
         )))

(defn upsert-cols!
  "set key's value to map 'm'. retains keys not provided in m"
  [repo key m]
  (d/transact! repo [(assoc m :gid key)]))

(defn build-repository
  [table-name]
  (let [schema {:gid {:db/unique :db.unique/identity}}
        repo (d/create-conn schema)
        metrics (atom {})]
    {:type            :datascript
     :repo            repo
     :metrics         metrics
     :close!          (fn [& _] (reset! repo {}))
     ;; :find            (partial #'find repo)
     ;; :find-all        #(find-all repo)
     ;; :query           (partial #'query repo)
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
;; (upsert-cols! (:repo r) "8.8.8.8" {:a 5})
;; (upsert-cols! (:repo r) "8.8.8.8" {:a 6})
;; (upsert-cols! (:repo r) "8.8.8.8" {:g 6})
;; (upsert-cols! (:repo r) "8.8.8.9" {:a 99 :g 6})
;; (entity->hash (:repo r) 1)
;; (find-aux (:repo r) [:gid "8.8.8.8"])
;; (find (:repo r) "8.8.8.8")
;; (d/q '[:find
;;        :where [?e :gid ]])
;; (query (:repo r) {:g 6})

