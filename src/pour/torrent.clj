(ns pour.torrent
  (:require [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]]
            [bencode.core :as bencode]))

(defn read-torrent-data
  [^java.io.PushbackInputStream s]
  (postwalk (fn [x]
              (if (instance? (clojure.lang.RT/classForName "[B") x)
                (try
                  (apply str (map char x))
                  (catch IllegalArgumentException _ x))
                x))
            (bencode/read-bencode s)))

(defn torrent-data [f]
  (with-open [s (java.io.PushbackInputStream. (io/input-stream f))]
    (read-torrent-data s)))
