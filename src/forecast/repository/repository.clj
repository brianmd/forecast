(ns forecast.repository.repository
  (:refer-clojure :exclude [find])
  (:require [forecast.helpers :as h]))

(defn metrics
  [repo]
  (:metrics repo))

(defn find
  [repo key]
  (h/bump (metrics repo) :find)
  ((:find repo) key))

(defn find-all
  [repo]
  (h/bump (metrics repo) :find-all)
  ((:find-all repo)))

(defn query
  [repo key-val]
  (h/bump (metrics repo) :query)
  ((:query repo) key-val)
  )

(defn upsert-cols!
  [repo key m]
  (h/bump (metrics repo) :upsert-cols!)
  ((:upsert-cols! repo) key m)
  )

