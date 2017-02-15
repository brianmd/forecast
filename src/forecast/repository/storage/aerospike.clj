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
  (log/info "0----------------- hey " @aero/conn-atom)
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
;; (upsert-cols! @aero/conn-atom "ip" "97.68.237.19" {"x" {:a 3}})
;; (find @aero/conn-atom "ip" "97.68.237.19")

(defn query
  [repo set key]
  (println "--------------   query:")
  (let [key-value (first key)
        ;; keyname (h/->keyname (first key-value))
        keyname (first key-value)
        value (second key-value)
        _ (prn keyname)
        _ (prn value)
        recs  (q/query repo
                       (q/mk-statement
                        {:ns "test" :set set}
                        ;; {:ns "test" :set set :index "ipid"}
                        (q/f-equal keyname value)))
        ]
    (log/info "count:" (count recs))
    (let [recs (map h/->map recs)]
      (into {} (map #(vector (read-string (:_id %)) (dissoc % :_id))) recs))
    ))
;; (query @aero/conn-atom "ip" {"state" "new"})
;; (first (query @aero/conn-atom "ip" {"state" "new"}))
;; (count (query @aero/conn-atom "ip" {"state" "new"}))

;; (setup! nil)
;; (def s (q/mk-statement {:ns "test" :set "ip" :index "ipstate"}
;; ;; (def s (q/mk-statement {:ns "test" :set "ip" }
;;                        (q/f-equal "state" "new")))
;; (def qu (q/query @aero/conn-atom s))
;; (count qu)
;; (type qu)
;; (type (first qu))
;; (map h/->map qu)

;; (doseq [method (sort-by #(.getName %) (.getMethods (class (.key (first x)))))]
;;   (println (.getName method) (seq (.getParameterTypes method))))

;; (doseq [method (sort-by #(.getName %) (.getMethods (class (first qu))))]
;;   (println (.getName method) (seq (.getParameterTypes method))))

;; (.toString (first qu))
;; (.record (first qu))
;; (.bins (.record (nth qu 1)))
;; (.key (nth qu 2))

(defn find-all
  [repo set]
    (let [m (aero/get repo "test" set)]
      ;; convert back to clojure-type keys
      ;; (if m (keywordize-keys (zipmap (.keySet m) (.values m))))
      (if m (keywordize-keys (into {} m)))
      ))
;; (aero/get @forecast.repository.ip-locator/ip-repo "test" "ip")


;; (setup!)

;; date (h/now)
;; m (cond-> (assoc m :id s)
;;     (not (contains? m :state)) (assoc :state "new"
;;                                       :stated-on date
;;                                       :created-on date)
;;     )

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

;; (query/query @repo (query/mk-statement {:ns "test" :set "location" :index "locationid"} (query/f-equal "id" "abcd99")))
;; (query/query @repo (query/mk-statement {:ns "test" :set "location" :index "location-ndx"} (query/f-equal "state" "new")))

;; (def x (q/query @aero/conn-atom (q/mk-statement {:ns "test" :set "ip"} (q/f-equal "state" "new"))))
;; (def x (q/query @aero/conn-atom (q/mk-statement {:ns "test" :set "ip"} (q/f-equal "id" "4"))))

;; (def x (q/query @repo (q/mk-statement {:ns "test" :set "location"} (q/f-equal "id" "abcd99"))))

;; (q/query @repo (q/mk-statement {:ns "test" :set "location"} (q/f-equal "id" "abcd99")))

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
;; (.key (first x))



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
  (log/info "creating indexes ....")
  ;; (q/create-index! repo "test" "ip"       "ipid"          "id"    :string)
  (prn repo)
  (q/create-index! repo "test" "ip"       "ipstate"       "state" :string)
  ;; (q/create-index! repo "test" "location" "locationid"    "id"    :string)
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
     :find-all        #(find-all repo set-name)
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



;; (close! nil)
;; (setup! forecast-initial-commands)
;; (def r @aero/conn-atom)
;; (upsert-cols! r "ip" :a {:state "blue" :id "a"})
;; (upsert-cols! r "ip" :b {:state "blue" :id "b"})
;; (upsert-cols! r "ip" :c {:state "blue" :id "c"})
;; (upsert-cols! r "ip" :d {:state "blue" :id "d"})
;; (query r "ip" {:state "blue"})

;; (upsert-cols! r "ip" :z {:state "green" :id "b"})
;; (query r "ip" {:state "green"})


;; recs  (q/query repo
;; (log/info "count:" (count recs))
;; (map h/->map recs)



;; get new connection, ensure state index exists
;; (close! nil)
;; (setup! forecast-initial-commands)
;; (def r @aero/conn-atom)

;; ;; insert rows
;; (.put r
;;       (WritePolicy.)
;;       (Key. "test" "ip" "r")
;;       (into-array Bin [(Bin. "state" "blue") (Bin. "id" "k")])
;;       )
;; (.put r
;;       (WritePolicy.)
;;       (Key. "test" "ip" "s")
;;       (into-array Bin [(Bin. "state" "blue") (Bin. "id" "s")])
;;       )
;; (.put r
;;       (WritePolicy.)
;;       (Key. "test" "ip" "t")
;;       (into-array Bin [(Bin. "state" "blue") (Bin. "id" "t")])
;;       )

;; ;; query for those rows
;; (do
;;   (def stmt (Statement.))
;;   (.setNamespace stmt "test")
;;   (.setSetName stmt "ip")
;;   (.setFilters stmt (into-array
;;                      com.aerospike.client.query.Filter
;;                      [(com.aerospike.client.query.Filter/equal "state" "blue")]
;;                      ))
;;   (def blue-query (.query r (com.aerospike.client.policy.QueryPolicy.) stmt))
;;   (def recs (doall (iterator-seq (.iterator blue-query))))
;;   recs
;;   )
;; (.iterator blue-query)
;; (count recs)
;; (first recs)
;; (-> (first recs) .key .namespace)
;; (-> (first recs) .key .setName)
;; (-> (first recs) .key .userKey)
