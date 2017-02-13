(ns forecast.repository.storage.aerospike
  (:refer-clojure :exclude [find])
  (:require [clojure.walk :refer [stringify-keys keywordize-keys]]
            [clojure.tools.logging :as log]
            [aeroclj.core :as aero]
            [aeroclj.query :as q]
            [forecast.helpers :as h]
            [forecast.metrics :refer [bump]]
            )
  (:import (com.aerospike.client AerospikeClient Key Bin Record Operation)
           (com.aerospike.client.policy WritePolicy ClientPolicy GenerationPolicy
                                        RecordExistsAction CommitLevel Policy BatchPolicy)
           (clojure.lang IPersistentMap)
           ))

(defn setup!
  [f]
  (when-not
   @aero/conn-atom
   (let [host (or (System/getenv "AEROSPIKE_HOST") "192.168.0.213")
         port (or (System/getenv "AEROSPIKE_PORT") 3000)
         repo (aero/connect! host port)]
      (reset! aero/conn-atom repo)
      (aero/init-once! repo "test" "test-set")
      (if f (f repo))))
  @aero/conn-atom)

(defn close!
  [_]
  (when @aero/conn-atom
    (try
      (aero/close! @aero/conn-atom)
      (catch Throwable e))
    (reset! aero/conn-atom nil)))

(defn find
  "finds record with id 'key'"
  [repo set key]
  (let [k (h/->keyname key)]
    (let [m (aero/get repo "test" set k)]
      ;; convert back to clojure-type keys
      ;; (if m (keywordize-keys (zipmap (.keySet m) (.values m))))
      (if m (keywordize-keys (into {} m)))
      )))

(defn query
  [repo set key]
  (let [find-hashmap (first key)
        recs  (q/query repo (q/mk-statement
                                 {:ns "test" :set set}
                                 (q/f-equal (first find-hashmap) (second find-hashmap))))
        ]
    (map h/->map recs)
    ))

(defn find-all
  [repo set]
    (let [m (aero/get repo "test" set)]
      ;; convert back to clojure-type keys
      ;; (if m (keywordize-keys (zipmap (.keySet m) (.values m))))
      (if m (keywordize-keys (into {} m)))
      ))
;; (aero/get @forecast.repository.ip-locator/ip-repo "test" "ip")

(defn upsert-cols!
  "set key's value to map 'm'. retains keys not provided in m"
  [repo set key m]
  (let [s (h/->keyname key)
        ;; date (h/now)
        ;; m (cond-> (assoc m :id s)
        ;;     (not (contains? m :state)) (assoc :state "new"
        ;;                                       :stated-on date
        ;;                                       :created-on date)
        ;;     )
        m (stringify-keys m)]
    (aero/put! repo "test" set s m)
    ))


;; (setup!)

;; (def repo (aero/connect! "192.168.0.213" 3000))
;; (reset! aero/conn-atom repo)
;; (aero/init-once! repo "test" "test-set")
;; (query/create-index! repo "test" "ip" "ip" "state" :string)
;; (query/create-index! repo "test" "ip" "location" "state" :string)


;; (aero/Key. "forecast" "test-set" "a")
;; (aero/->bin {"a" 3 "b" 4})

;; conn ns set key bins
;; (aero/put! repo "test" "test-set" "a" {"a" 3})
;; (aero/get repo "test" "test-set" "a")
;; (aero/put! repo "test" "test-set" (pr-str {"a" 99}) {"a" 3})
;; (aero/get repo "test" "test-set" (pr-str {"a" 99}))

;; (aero/put! "test3" {"bin3" "value3"})
;; (aero/get "test3")
;; (aero/get repo "test" "test-set" "test3")


;; aero/ns-atom
;; aero/set-atom
;; aero/conn-atom


;; (.put conn *wp* (mk-key ns set key) (->bin bins))
;; ;;                             ns      set     key
;; (.put repo aero/*wp* (Key. "forecast" "test" "test")
;;       (->bin bins))

;; (aero/put! "forecast" "test" )

;; ns set key bin

;; (aero/put! @repo "test" "ip" "123" {"a" "7"})
;; (aero/put! @repo "test" "ip" ip location)
;; (aero/get @repo "test" "ip" ip)
;; (aero/put! @repo "test" "location" (pr-str location) {"temp" forecast})


;; (upsert! "location" {:x 4} {:z 4 :u {:t 17}})
;; (find "location" {:x 4})
;; (aero/get repo "test" "location" (pr-str {:x 4}))
;; (stringify-keys {:x 4})

;; (clear)

;; (find "location" {:x 4})
;; (find-location {:x 4})



;; (setup!)


;; (defn upsert!
;;   "set key's value to map 'm'. removes keys not provided in m.'"
;;   [repo set key m]
;;   (let [s (h/->keyname key)
;;         date (h/now)
;;         m (cond-> (assoc m :id s)
;;             (not (contains? m :state)) (assoc :state "new"
;;                                               :stated-on date
;;                                               :created-on date)
;;             )
;;         m (stringify-keys m)]
;;     (aero/put! repo "test" set s m)
;;     ))

;; (defn upsert-keys-for!
;;   [repo set key current-m m]
;;   (upsert! set (h/->keyname key) (merge current-m m)))

;; (defn find-new
;;   [set]
;;   (let [recs  (q/query repo (q/mk-statement {:ns "test" :set set} (q/f-equal "state" "new")))
;;         ;; bs (if (and recs (first recs)) (.bins (.record (first recs))))
;;         ;; key (if recs (.))
;;         ]
;;     ;; (hash->map bs)
;;     ;; (if bs (keywordize-keys (zipmap (.keySet bs) (.values bs))))
;;     (map h/->map recs)
;;     ))
;; ;; (find-new "location")
;; ;; (map :id (find-new "location"))

;; (defn find-first-new
;;   [repo set]
;;   (let [recs  (q/query repo (q/mk-statement {:ns "test" :set set} (q/f-equal "state" "new")))
;;         bs (if (and recs (first recs)) (.bins (.record (first recs))))
;;         ;; key (if recs (.))
;;         ]
;;     (h/hash->map bs)
;;     ;; (if bs (keywordize-keys (zipmap (.keySet bs) (.values bs))))
;;     ))

;; (defn upsert-keys!
;;   "updates keys from map while retaining bins not in map"
;;   [set key m]
;;   (let [current-m (find set key)]
;;     (upsert-keys-for! set key current-m m)))




;; (find "location" {:x 4})
;; (update-bins! "location" {:x 4})
;; (upsert! "location" {:x 5 :y "asdferwre"} {:t 4 :x 7})
;; (upsert! "location" "abcd99" {:z (str (rand))})

;; (find "location" {:x 5})
;; (find "location" {:x 5 :y "asdferwre"})
;; (upsert! "test-demo" "4" {"a" 2})
;; (upsert-location! {:x 5} 3)

;; (find-first-new "location")
;; (:z (find-first-new "location"))

;; ;; (query/query @repo (query/mk-statement {:ns "test" :set "location" :index "locationid"} (query/f-equal "id" "abcd99")))
;; ;; (query/query @repo (query/mk-statement {:ns "test" :set "location" :index "location-ndx"} (query/f-equal "state" "new")))
;; (def x (query/query @repo (query/mk-statement {:ns "test" :set "location"} (query/f-equal "state" "new"))))
;; (def x (query/query @repo (query/mk-statement {:ns "test" :set "location"} (query/f-equal "id" "abcd99"))))

;; (query/query @repo (query/mk-statement {:ns "test" :set "location"} (query/f-equal "id" "abcd99")))

;; (first x)
;; (count x)
;; (second x)
;; (.generation (first x))
;; (.expiration (first x))
;; (.expiration (first x))

;; (setup!)
;; (upsert! "location" "test1" {:z 4})
;; (find-first-new "location")
;; (.bins (.record (first x)))



;; (def x (query/query @repo (query/mk-statement {:ns "test" :set "location"} (query/f-equal "state" "new"))))
;; (type x)
;; (.key (first x))
;; (.key (last x))
;; (map (fn [r] (.userKey (.key r))) x)
;; (map (fn [r] (.bins (.record r))) x)
;; (map (fn [z] (z "id")) (map (fn [r] (.bins (.record r))) x))
;; (find-first-new "location")
;; (map :id (find-first-new "location"))
;; (aero/delete! @repo "test" "location" "abcd99")
;; (doseq [id (map :id (find-new "location"))]
;;   (aero/delete! @repo "test" "location" id))
;; (count x)
;; (.toString (.key (first x)))
;; (.namespace (.key (first x)))
;; (.setName (.key (first x)))
;; (.userKey (.key (first x)))

;; (.record (first x))
;; (.expiration (.record (first x)))
;; (.generation (.record (first x)))
;; (.bins (.record (first x)))
;; (.bins (.record (second x)))

;; (class 3)
;; (type 3)
;; (supers (class 3))
;; (supers (class ""))
;; (vec (.getMethods String))
;; (map o)(vec (.getMethods String))

;; (doseq [method (sort-by #(.getName %) (.getMethods String))]
;;   (println (.getName method) (seq (.getParameterTypes method))))

;; (doseq [method (sort-by #(.getName %) (.getMethods java.nio.charset.StandardCharsets))]
;;   (println (.getName method) (seq (.getParameterTypes method))))



;; (let [bs (.bins (.record (first x)))]
;;   (zipmap (.keySet bs) (.values bs)))

;; (type (.bins (.record (first x))))
;; (get (.bins (.record (first x))) "z")
;; (get (.bins (.record (first x))) "z")
;; (query/query @repo (query/mk-statement {:ns "test" :set "location" :index "location-ndx"} ))
;; (setup!)

;; (defn clear-ips [] (reset! ips {}))

;; (defn clear-locations [] (reset! locations {}))

;; (defn clear
;;   []
;;   (clear-ips)
;;   (clear-locations))

;; (defn upsert-ip!
;;   [ip location]
;;   (bump [:ip :inserts])
;;   (upsert! "ip" ip location)
;;   )

;; (defn find-ip
;;   [ip]
;;   (bump [:ip :finds])
;;   (find "ip" ip)
;;   )

;; (defn all-locations
;;   []
;;   (vals @ips))

;; (defn upsert-location!
;;   [location forecast]
;;   (bump [:location :inserts])
;;   (upsert! "location" location {"temp" forecast})
;;   )

;; (defn find-location
;;   [location]
;;   (bump [:location :finds])
;;   (let [temp (find "location" location)]
;;     (get temp :temp)))

;; (defn all-temperatures
;;   []
;;   (vals @locations))

(defn forecast-initial-commands
  [repo]
  (q/create-index! repo "test" "ip"       "ipid"          "id"    :string)
  (q/create-index! repo "test" "ip"       "ipstate"       "state" :string)
  (q/create-index! repo "test" "location" "locationid"    "id"    :string)
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
     :find            (partial find repo set-name)
     :find-all        #(find-all repo set-name)
     :query           (partial query repo set-name)
     :find-seq        identity
     :find-all-seq    identity
     :insert!         identity
     :update-replace! identity
     :update-cols!    (partial upsert-cols! repo set-name)
     :upsert-replace! identity
     :upsert-cols!    (partial upsert-cols! repo set-name)
     :delete!         identity
     :delete-all!     identity
     }))

;; (close! nil)
