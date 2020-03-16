(ns pour.client
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

(defn- build-client
  [builder {::keys [magnet dir]}]
  (let [path (java.nio.file.Paths/get (java.net.URI. (str "file://" dir)))]
    (.. builder
        (storage (FileSystemStorage. path))
        (magnet magnet)
        build)))

(def ^:private bt-client :bt/BtClient)
(def ^:private callback-set ::callback-set)

(defn new-client
  ([opts]
   {bt-client (build-client (Bt/client) opts)
    callback-set (atom (into #{} (get opts ::callbacks)))})
  ([runtime opts]
   {bt-client (build-client (Bt/client runtime) opts)
    callback-set (atom (into #{} (get opts ::callbacks)))}))

(defn start!
  [client]
  (assoc client ::completable-future
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

(comment

  (do
    (def opts {::dir "/home/hy/projects/pour/data"
               ::magnet "magnet:?xt=urn:btih:cf3b8d5ecdd4284eb9b3a80fcfe9b1d621548f72&tr=http%3A%2F%2Facademictorrents.com%2Fannounce.php&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969"})
    
    (def client (new-client opts))
    
    (def cb (fn [state-map] (clojure.pprint/pprint state-map))))

  (add-callback! client cb)

  (start! client)

  (stop! client)
  
  )
