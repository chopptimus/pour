(ns pour.core
  (:import
   [bt Bt]
   [bt.data.file FileSystemStorage]))

(def options
  #::{:dir "/home/hy/.local/tmp"
      :magnet "magnet:?xt=urn:btih:af0d9aa01a9ae123a73802cfa58ccaf355eb19f1"})

(defn print-session-state
  [session-state]
  (println (.getDownloaded session-state)))

(defn client
  [{::keys [dir magnet]}]
  (let [path (java.nio.file.Paths/get (java.net.URI. (str "file://" dir)))
        s (FileSystemStorage. path)]
    (.. (Bt/client)
        (storage s)
        (magnet magnet)
        build)))

(defn run
  [client callback]
  (.startAsync
   client
   (reify java.util.function.Consumer
     (accept [_ session-state]
       (callback session-state))
     (andThen [_ consumer]
       consumer))
   5000))
