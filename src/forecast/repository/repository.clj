(ns forecast.repository.repository
  (:refer-clojure :exclude [find])
  (:require [forecast.helpers :as h]))

(defn metrics
  [repo]
  (:metrics repo))

(defn repo
  [repo]
  (:repo repo))

(defn find
  [repo key]
  (h/bump (metrics repo) :find)
  ((:find repo) key))

(defn find-all
  [repo]
  (h/bump (metrics repo) :find-all)
  ((:find-all repo)))

(defn upsert-cols!
  [repo key m]
  (h/bump (metrics repo) :upsert-cols!)
  ((:upsert-cols! repo) key m)
  )


;; (:upsert-cols! r)
;; (:repo r)

;; (def r (forecast.repository.storage.memory/build-repository "ip"))
;; (upsert-cols! r :x {:f 4})
;; (upsert-cols! r :x {:g 4})
;; (find r :x)
;; (find-all r)

