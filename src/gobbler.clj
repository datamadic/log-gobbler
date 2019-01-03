(ns gobbler
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s])
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
         (println (json/write-str (merge meta {:timestamp timestamp
                                               :message ln})))))))

(def meta-info-matchers
  {:command_line_args #"Command Line: (.*)"
   :manifest #"--startup-url=(.*?)(?: [\-]{0,2}|$)"
   :core_sha #"core\": \"(.*)\""
   :node-version #"node\": \"(.*)\""
   :v8_version #"v8\": \"(.*)\""
   :openfin_version #"openfin\": \"(.*)\""
   :adapter_version #"adapter\": \"(.*)\""
   :electron_version #"electron\": \"(.*)\""})

;; accretion of values with reduce..



;; (map (fn [[x]] x) meta-info-matchers)

;; (defrecord meta-info-reduce-seed [matched matchers])

;; bail out with reduced if all matched, return the matches
(defn meta-info-reducer [[matched  matchers] line]
  ;; (println matched)
  ;;   (println line)
  [(into {} (map (fn [[k v]]
                   (if (nil? v)
                     [k (last (re-find (k matchers) line))]
                     [k v]))
                 matched))
   matchers])

(defn -main [& stuff]
  (let [[infile outfile] stuff

        ;; change the uuid to a hash of the file
        log-uuid (.toString (java.util.UUID/randomUUID))
        [meta]  (with-open [rdr (clojure.java.io/reader infile)]

                        (reduce meta-info-reducer
                                [(into {}
                                       (map vector
                                            (keys meta-info-matchers)
                                            (repeat (count meta-info-matchers) nil)))
                                 meta-info-matchers]
                                (line-seq rdr))
                        )]



    ;; (println meta )
    ;; (System/exit 0)

    (with-open [rdr (clojure.java.io/reader infile)]
      (doseq  [ln  (line-seq rdr)]
        (make-es ln (merge meta  {:log-uuid log-uuid})))
      (println ""))))

;; (get (into {} (map vector lst (repeat (count lst) "waka"))) 2)

(def t "[11/20/18 10:58:57]-[INFO:atom_api_app.cc(1203)] 11/20 10:58:57.261 - group-policy build: true")
(re-find #"\d+\/\d+\/\d+ \d+:\d+:\d+" t)
(re-find #"(VERBOSE|VERBOSE1|INFO|ERROR|WARNING):.*\]" t)
;; ["INFO:atom_api_app.cc(1203)]" "INFO"]
;; curl -XPOST "http://localhost:9200/logs/_doc/_bulk?pretty" -H 'Content-Type: application/json' --data-binary @lines.json
;; (defn uuid [] (.toString (java.util.UUID/randomUUID)))

;; clojure.pprint/pprint

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
