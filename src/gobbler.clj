(ns gobbler
  (:require [clojure.data.json :as json])
  (:gen-class))

;; (println *command-line-args*)

;; reduce to catch the last got log
;; write in the dir the mappings

(def ts-reg #"\d+\/\d+\/\d+ \d+:\d+:\d+")

(defn make-es
  ([ln] (make-es ln {}))
  ([ln meta]
   (if-let [timestamp (re-find ts-reg ln)] 
     (do (println "{\"index\":{}}")
         (println (json/write-str (merge meta {
                                               :timestamp timestamp
                                               :message ln
                                               })))))))

;; get the las here
(def meta-info-matchers
  {:command_line_args #"Command Line: (.*)"
   :manifest #"--startup-url=(.*?)(?: [\-]{0,2}|$)"
   :core_sha #"core\": \"(.*)\""
   :node-version #"node\": \"(.*)\""
   :v8_version #"v8\": \"(.*)\""
   :openfin_version #"openfin\": \"(.*)\""
   :adapter_version #"adapter\": \"(.*)\""
   :electron_version #"electron\": \"(.*)\""})


;; "http_parser": "2.7.0",
;; "node": "8.9.3",
;; "v8": "6.1.534.41",
;; "uv": "1.15.0",
;; "zlib": "1.2.11",
;; "ares": "1.10.1-DEV",
;; "modules": "57",
;; "nghttp2": "1.25.0",
;; "openssl": "1.0.2n",
;; "openfin": "9.61.38.27",
;; "chrome": "61.0.3163.100",
;; "adapter": "6f9b2c3dc47b376dbb1572ff6573fecefdca409e",
;; "core": "252627ef3a772d5ab37b03a2da604eb109a588e7",
;; "electron": "2.0.7"

;; accretion of values with reduce.. 



;; (map (fn [[x]] x) meta-info-matchers)

(defrecord meta-info-reduce-seed [matched matchers])
(defn meta-info-reducer [{matched :matched matchers :matchers } line]
  ;; (println matched)
  ;;   (println line)
  (->meta-info-reduce-seed (into {} (map (fn [[k v]]
                                   (if (nil? v)
                                     [k (last (re-find (k matchers) line))]
                                     [k v]))
                                 matched))
                           matchers))

(defn -main [& stuff]
  (let [[infile outfile] stuff

        ;; change the uuid to a hash of the file
        log-uuid (.toString (java.util.UUID/randomUUID))
        meta "foo"]

    (with-open [rdr (clojure.java.io/reader infile)]
      
      (clojure.pprint/pprint (:matched (reduce meta-info-reducer
                        (->meta-info-reduce-seed (into {}
                                                       (map vector
                                                            (keys meta-info-matchers)
                                                            (repeat (count meta-info-matchers) nil)))
                                                 meta-info-matchers)
                        (line-seq rdr))))
      )

    ;; (println meta )
    (System/exit 0)

    (with-open [rdr (clojure.java.io/reader infile)]
      (doseq  [ln  (line-seq rdr)]
        (make-es ln (merge {}  {:log-uuid log-uuid})))
      (println ""))))

;; (get (into {} (map vector lst (repeat (count lst) "waka"))) 2)

(def t "[11/20/18 10:58:57]-[INFO:atom_api_app.cc(1203)] 11/20 10:58:57.261 - group-policy build: true")
(re-find #"\d+\/\d+\/\d+ \d+:\d+:\d+" t)
(re-find #"(VERBOSE|VERBOSE1|INFO|ERROR|WARNING):.*\]" t)
;; ["INFO:atom_api_app.cc(1203)]" "INFO"]
;; curl -XPOST "http://localhost:9200/logs/_doc/_bulk?pretty" -H 'Content-Type: application/json' --data-binary @lines.json
;; (defn uuid [] (.toString (java.util.UUID/randomUUID)))



;; dorun, doall, and doseq are all for forcing lazy sequences, presumably to get side effects.

;; dorun - don't hold whole seq in memory while forcing, return nil
;; doall - hold whole seq in memory while forcing (i.e. all of it) and return the seq
;; doseq - same as dorun, but gives you chance to do something with each element as it's forced; returns nil

;; (System/getenv "VAR")

;; clojure clj -m vs -i
;;https://clojure.org/guides/deps_and_cli
;; https://clojure.org/reference/compilation

;; https://lethain.com/reading-file-in-clojure/

;; curl -XPOST "http://localhost:9200/logs/_doc/_bulk?pretty" -H 'Content-Type: application/json' --data-binary @lines.json
;; clj -m gobbler "/Users/Admin/Downloads/Logs Diego/New Compressed (zipped) Folder/debugl181121104643.log" > lines.json

;; [01/02/2019 15:33:27]-[VERBOSE1:atom_main_delegate.cc(306)] Command Line: --v=1 --inspect --startup-url=http://localhost:3449/app.json --framestrategy=frames --no-sandbox --disabled-frame-groups --process-log-file=debugp099030.log
