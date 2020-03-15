(ns pour.core
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

(defn print-session-state
  [session-state]
  (clojure.pprint/pprint (session-state->map session-state)))

(defn client
  [dir magnet]
  (let [path (java.nio.file.Paths/get (java.net.URI. (str "file://" dir)))]
    (.. (Bt/client)
        (storage (FileSystemStorage. path))
        (magnet magnet)
        build)))

(defn run
  [client]
  (.startAsync
   client
   (reify java.util.function.Consumer
     (accept [_ session-state]
       (callback session-state))
     (andThen [_ consumer]
       consumer))
   1000))
