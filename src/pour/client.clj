(ns pour.client
  (:require
   [clojure.java.io :as io])
  (:import
   [bt Bt]
   [bt.data.file FileSystemStorage]))

(defn session-state->map
  [session-state]
  {::connected-peers (into #{} (seq (.getConnectedPeers session-state)))
   ::downloaded (.getDownloaded session-state)
   ::uploaded (.getUploaded session-state)
   ::pieces-complete (.getPiecesComplete session-state)
   ::pieces-incomplete (.getPiecesIncomplete session-state)
   ::pieces-not-skipped (.getPiecesNotSkipped session-state)
   ::pieces-remaining (.getPiecesRemaining session-state)
   ::pieces-total (.getPiecesTotal session-state)})

(defn- build-torrent-client
  [builder torrent storage _]
  (.. builder
      (storage (FileSystemStorage. (.toPath (io/as-file storage))))
      (torrent (.. (io/as-file torrent) toURI toURL))
      build))

(defn- build-magnet-torrent-client
  [builder magnet storage _]
  (.. builder
      (storage (FileSystemStorage. (.toPath (io/as-file storage))))
      (magnet magnet)
      build))

(def ^:private bt-client :BtClient)
(def ^:private callback-set :callback-set)

(defn new-torrent
  [torrent storage opts]
  (if (and (string? torrent) (= (subs torrent 0 7) "magnet:"))
    {bt-client (build-magnet-torrent-client (Bt/client) torrent storage opts)
     callback-set (atom (into #{} (get opts ::callbacks)))} 
    {bt-client (build-torrent-client (Bt/client) torrent storage opts)
     callback-set (atom (into #{} (get opts ::callbacks)))}))

(defn start!
  [client]
  (assoc client :completable-future
         (.startAsync
          (bt-client client)
          (reify java.util.function.Consumer
            (accept [_ session-state]
              (let [callbacks @(callback-set client)
                    m (session-state->map session-state)]
                (doseq [f callbacks]
                  (try
                    (f m)
                    (catch Throwable ex (println (str ex)))))))
            (andThen [_ consumer]
              consumer))
          100)))

(defn started?
  [client]
  (.isStarted (bt-client client)))

(defn stop!
  [client]
  (.stop (bt-client client)))

(defn add-callback!
  [client callback]
  (update client callback-set swap! conj callback))

(defn remove-callback!
  [client callback]
  (update client callback-set swap! disj callback))
