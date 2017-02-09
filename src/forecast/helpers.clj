(ns forecast.helpers
  (:import [org.jfree.data.statistics HistogramDataset]))

(def ip-regex
  #"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")

(defn valid-ip?
  [ip]
  (not (nil? (re-matches ip-regex ip))))

(defn histogram
  [data num-bins]
  (let [hist (HistogramDataset.)]
    (.addSeries hist "x" (double-array data) num-bins)
    (map #(vector (.getStartX hist 0 %) (.getEndX hist 0 %) (.getY hist 0 %)) (range (.getItemCount hist 0)))
    ))
;; (histogram [1 7 22 4 2 19] 5)

(defn log-error
  [msg value]
  (println msg value))

