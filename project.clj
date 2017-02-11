(defproject forecast "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/core.async "0.2.395"]
                 [clj-http "2.3.0"]
                 [incanter "1.5.7"]   ;; R-like library
                 [cheshire "5.7.0"]   ;; json
                 [aeroclj "0.1.1"]
                 ;; [com.aerospike/aerospike-client "3.0.9"]
                 ]
  :main ^:skip-aot forecast.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
