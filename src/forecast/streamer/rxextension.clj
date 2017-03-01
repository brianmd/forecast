(ns forecast.streamer.rxextension
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            ;; [rx.lang.clojure.core :as rxx]
            ;; [rx.lang.clojure.interop :as inter]
            [beicon.core :as rx]
            [clojure.core.async :as a]
            [cheshire.core :as json]

            [forecast.helpers :as h :refer [valid-ip? bump now]]
            [forecast.repository.ip-locator :as ip-service]
            [forecast.repository.location-forecast :as locate-service]
            )
  ;; (:import [rx Observable]
  ;;          [java.util.concurrent TimeUnit]
  ;;          )
  )

(defn read-file
  [filename out-chan]
  (a/go
    (with-open [rdr (clojure.java.io/reader (str "data/" filename))]
      (doseq [line (line-seq rdr)]
        (println "line: " line)
        (a/>! out-chan line)))
    (a/close! out-chan)))

(defn parse-line
  [line]
  (->
   line
   (string/split #"\t")
   (nth 23)          ;; ip address
   ))

;; (let [line-chan (a/chan)
;;       ip        (a/chan)
;;       unique-ip (a/chan)
;;       valid-ip (a/chan)
;;       location (a/chan)
;;       unique-location (a/chan)
;;       find-temp (a/chan)
;;       out (a/chan)
;;       ]
;;   (read-file "logfile" line-chan)
;;   (a/pipeline-blocking 1 ip       (map #(parse-line %))                       line-chan)
;;   (a/pipeline-blocking 1 valid-ip (filter #(valid-ip? %))                     ip)
;;   (a/pipeline-async    1 location (map #(@ip-service/locate-service %))       valid-ip)
;;   ;; (a/pipeline-async    1 location (map #(@locate-service/forecast-service %)) location)

;;   (a/pipeline-blocking 1 out (map identity) location)
;;   (a/<!! (a/into [] out))
;;   ;; (a/<!! (a/into [] line-chan))
;;   )
;; ;; (@ip-service/locate-service 3)
;; ;; (a/put! (a/chan) {:a 4})
;; ;; (let [c (a/chan)]
;; ;;   (a/put! c {:a 4})
;; ;;   (a/take! c #(println %)))


;; (defn process
;;   [filename-chan histogram-chan]
;;   (let [line      (chan)
;;         ip        (chan)
;;         unique-ip (chan)
;;         lookup-ip (chan)
;;         unique-location (chan)
;;         find-temp (chan)]
;;     (a/pipeline-blocking 1 filename-chan read-file line)
;;     (a/pipeline-blocking 1 line extract-ip ip)
;;     (a/pipeline-blocking 1 ip keep-new unique-ip)
;;     (a/pipeline-async    1 unique-ip find-location location)
;;     (a/pipeline-blocking 1 location keep-new unique-location)
;;     (a/pipeline-async    1 unique-location find-temp temp)
;;     (a/pipeline-blocking 1 temp build-histogram histogram-chan)
;;     ))
;; (let [filename-chan (chan)
;;       histogram-chan (chan)]
;;   (a/>!! filename "data/logfile")
;;   (a/close! filename)
;;   (process filename-chan)
;;   (show-histogram)
;;   )


(defn make-source
  []
  )

(defn make-sink
  []
  )

(defn call
  [cog fn-name & args]
  (prn [fn-name args])
  (let [f (-> @cog :fns fn-name)]
    (prn f)
    (apply f args)))

(defn make-cog
  ([] (make-cog 5))
  ([n]
   (let [in (a/chan n)
         out (a/chan n)
         mult-out (a/mult out)
         err (a/chan n)
         cog (atom {:in in
                    :out out
                    :mult-out mult-out
                    :err err
                    })
         ]
     (swap! cog
            assoc
            :fns
            {:on-input identity
             :on-close identity

             :test identity

             :process (a/go-loop []
                        (println "in go-loop")
                        (let [in-val (a/<! in)]
                          (prn ["in-val" in-val])
                          (prn )
                          (try
                            (let [v (call cog :on-input in-val)]
                              (prn ["on-input v" v])
                              (a/put! out v)
                              (prn ["has put v to out" v])
                              )
                            (catch Throwable e
                              ;; (log/info e "error in cog-process, in-val=" in-val)
                              (println "got an error" e)
                              (a/>! err {:error e :in-val in-val})))
                          (when (-> cog :fns :closed?) (recur))
                          ))
             :closed? false
             :close! (fn []
                       (println "closing")
                       (swap! cog assoc-in [:fns :closed?] true)
                       (a/close! in)
                       (a/close! out)
                       (a/close! err)
                       )
             })
     cog)))



;; (def c (a/chan 5))
;; (def o (a/chan 5))
;; ;; (def mc (a/mult (a/chan 5)))
;; (def mc (a/mult c))
;; (a/tap mc o)
;; (a/put! c "sent to allll")
;; (a/go (println (a/<! o)))



(defn close-cog
  [cog]
  ((-> @cog :fns :close!)))

(defn <!
  [cog]
  (a/<! (:out @cog)))
(defn <!!
  [cog]
  (a/<!! (:out @cog)))

(defn >!
  [cog v]
  (a/>! (:in @cog) v))
(defn >!!
  [cog v]
  (a/>!! (:in @cog) v))

(defn connect!
  [from-cog to-cog]
  (a/tap (:mult-out @from-cog) (:in @to-cog)))

;; (def a (make-cog 5))
;; (def b (make-cog 5))
;; (connect! a b)
;; (call a :test 3)
;; (close-cog a)
;; (close-cog b)
;; (-> @a :fns :close!)
;; (-> @a :in)
;; (-> @a :out)

;; (>!! a 7)
;; (<!! b)


;; (def stream (rx/from-coll [1 2 3]))
;; (rx/on-value stream prn)

;; (def stream (rx/from-coll (range 10)))
;; (def stream2 (rx/map #(* 5 %) stream))
;; (rx/on-value stream2 prn)

;; (def a (atom 1))
;; (def stream (rx/from-atom a))
;; (rx/on-value stream prn)
;; (swap! a inc)
;; (reset! a {:a 2 :b 7})
;; (swap! a update :a inc)

;; (def stream (rx/of 1 2 3))
;; (rx/on-value stream prn)

;; (rx/timeout 1000 10)
;; (def stream (rx/timout 1000 :a))
;; (rx/on-value stream prn)

;; (def stream
;;   (rx/create (fn [sink]
;;                (sink 1)          ;; next with `1` as value
;;                (sink :x)          ;; next with `1` as value
;;                (throw (Exception. "err in stream"))
;;                (sink (rx/end 2)) ;; next with `2` as value and end the stream
;;                (fn []
;;                  ;; function called on unsubscription
;;                  (println "unsubscribed")
;;                  ))))
;; (rx/on-value stream #(println "v:" %))

;; (def stream (->> (rx/from-coll [1 2 3 4])
;;                  (rx/buffer 2 1)))
;; (rx/on-value stream prn)

;; (def stream (rx/choice
;;              (rx/timeout 1000 :timeout)
;;              (rx/timeout 900 :value)))

;; (def natom (atom 3))
;; (def stream
;;   (rx/generate (fn [state sink]
;;                  (let [nextval (inc state)]
;;                    (log/info "nextval:" nextval)
;;                    (Thread/sleep 1000)
;;                    (if (< @nextval 10)
;;                      (sink nextval)
;;                      (rx/end 999))
;;                    nextval))
;;                ;; #(do
;;                ;;    (Thread/sleep 1000)
;;                ;;    (if (< @natom 10)
;;                ;;      (swap! natom inc)
;;                ;;      )
;;                ;;    )
;;                (constantly 0)
;;                ))
;; (rx/on-value stream prn)
;; (log/info 3)





;; (def repl-out *out*)
;; (defn prn-to-repl [& args]
;;   (binding [*out* repl-out]
;;     (apply prn args)))

;; (def obs (rx/return 10))
;; (rx/subscribe obs
;;               (fn [value]
;;                 (prn (str "Got value: " value))))

;; (do
;;   (def subscription
;;     (rx/subscribe (Observable/interval 100 TimeUnit/MILLISECONDS)
;;                   prn-to-repl))
;;   (Thread/sleep 1000)
;;   (rx/unsubscribe subscription)
;;   )

;; (defn just-obs [v]
;;   (rx/observable*
;;    (fn [observer]
;;      (rx/on-next observer v)
;;      (rx/on-completed observer))))
;; (rx/subscribe (just-obs 20) prn)
;; (rx/subscribe (just-obs 20) prn-to-repl)

;; (rx/subscribe (->> (Observable/interval 1 TimeUnit/MICROSECONDS)
;;                    ;; (rx/filter even?)
;;                    (rx/take 5)
;;                    (rx/reduce +))
;;               prn-to-repl)



;; (defn musicians []
;;   (rx/seq->o ["James Hetfield" "Dave Mustaine" "Kerr King"]))
;; (defn bands []
;;   (rx/seq->o ["Metallica" "Megadeth" "Slayer"]))
;; (defn uppercased-obs []
;;   (rx/map (inter/fn [s] (.toUppercase s)) (bands)))
;; (rx/subscribe (musicians) prn-to-repl)
;; (rx/subscribe (bands) prn-to-repl)
;; (rx/subscribe (uppercased-obs) prn-to-repl)
;; (-> (rx/map vector
;;             (musicians)
;;             (uppercased-obs))
;;     (rx/subscribe (fn [[musician band]]
;;                     (prn-to-repl (str musician " - from: " band)))))
