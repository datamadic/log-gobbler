(ns gobbler
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s])
  (:gen-class))
;; should make it handle both log formats (1/2/3 vs 1-2-3)
(def v14-wtl "[2019-12-30 11:30:02.653]-[VERBOSE1:atom_api_app.cc(1541)] received in-runtime: 1 [OpenfinPOC]-[OpenfinPOC] {\"action\":\"write-to-log\",\"isSync\":false,\"payload\":{\"level\":\"info\",\"message\":\"[licenseKey] [OpenfinPOC - OpenfinPOC]: invalid OpenFin license key\"},\"singleFrameOnly\":true}")
(def v14-animate "[2019-12-30 11:30:34.588]-[VERBOSE1:atom_api_app.cc(1541)] received in-runtime: 14 [OpenfinPOC]-[queueCounter] {\"action\":\"animate-window\",\"payload\":{\"name\":\"queueCounter\",\"transitions\":{\"position\":{\"duration\":300,\"left\":1130,\"top\":870}},\"uuid\":\"OpenfinPOC\"}}")



(def ts-reg-v10 #"\d+\/\d+\/\d+ \d+:\d+:\d+") ;; unused
(def ts-reg-v14 #"(\d+\-\d+\-\d+ \d+:\d+:\d+(?:\.\d+)?)")
(def message-v14 #"(?:\d+\-\d+\-\d+ \d+:\d+:\d+(?:\.\d+)?)(.*)")
(def meta-info-matchers
  {:command_line_args #"Command Line: (.*)"
   :manifest #"--startup-url=(.*?)(?: [\-]{0,2}|$)"
   :core_sha #"core\": \"(.*)\""
   :node-version #"node\": \"(.*)\""
   :v8_version #"v8\": \"(.*)\""
   :openfin_version #"openfin\": \"(.*)\""
   :adapter_version #"adapter\": \"(.*)\""
   :electron_version #"electron\": \"(.*)\""})

;; build these as strings??
;; make key an array of terms to line up with multiple capture groups??
(def info-matchers
  {:timestamp ts-reg-v14
   :app #"received in-runtime: \d+ \[(.*?)]"
   :win #"received in-runtime: \d+ \[(?:.*?)]-\[(.*?)]"
   :frame_id #"received in-runtime: (\d+)"
   :action #"received in-runtime: \d+ \[(?:.*?)]-\[(?:.*?)] \{\"action\":\"(.*?)\""
   :message #"(.*)"})
(def empty-index "{\"index\":{}}")

(defn nil-map [m]
  (into {}
        (map vector
             (keys m)
             (repeat (count m) nil))))
(s/fdef nil-map
        :args (s/cat :m map?)
        :ret map?)

(defn map-map [f m] (into {} (map (fn [[k v]] [k (f v)]) m))) ;; unused

;; this should not do the printing...
;; extract the (into {} (map ... ))
(defn make-es
  ([ln] (make-es ln {}))
  ([ln meta]
   (let [fields (into {} (map (fn [[k v]] [k (last (re-find v ln))]) info-matchers))]
     (if (not (nil? (:timestamp fields)))
         (do (println empty-index)
             (println (json/write-str (merge meta fields))))))))


(defn meta-info-reducer [[matched  matchers :as reduction] line]
  (if (every? (fn [[_ v]] v) matched)
    (reduced reduction)
    [(into {} (map (fn [[k v]]
                     (if (nil? v)
                       [k (last (re-find (k matchers) line))]
                       [k v]))
                   matched))
     matchers]))

(defn -main [& stuff]
  (let [[infile outfile] stuff

        ;; change the uuid to a hash of the file
        log-uuid (.toString (java.util.UUID/randomUUID))
        [meta]  (with-open [rdr (clojure.java.io/reader infile)]
                  (reduce meta-info-reducer
                          [(nil-map meta-info-matchers) meta-info-matchers]
                          (line-seq rdr)))]

    (with-open [rdr (clojure.java.io/reader infile)]
      (doseq  [ln  (line-seq rdr)]
        (make-es ln (merge meta  {:log-uuid log-uuid})))
      (println ""))))













;; looked up times... 

;; (System/exit 0)
;; (get (into {} (map vector lst (repeat (count lst) "waka"))) 2)

;; (def t "[11/20/18 10:58:57]-[INFO:atom_api_app.cc(1203)] 11/20 10:58:57.261 - group-policy build: true")
;; (re-find #"\d+\/\d+\/\d+ \d+:\d+:\d+" t)
;; (re-find #"(VERBOSE|VERBOSE1|INFO|ERROR|WARNING):.*\]" t)
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

;; To have launchd start grafana now and restart at login:
;; brew services start grafana
;; Or, if you don't want/need a background service you can just run:
;; grafana-server --config=/usr/local/etc/grafana/grafana.ini --homepath /usr/local/share/grafana cfg:default.paths.logs=/usr/local/var/log/grafana cfg:default.paths.data=/usr/local/var/lib/grafana cfg:default.paths.plugins=/usr/local/var/lib/grafana/plugins
