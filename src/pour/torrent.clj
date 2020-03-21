(ns pour.torrent
  (:require [clojure.walk :refer [postwalk]]
            [bencode.core :as bencode]))

(defn torrent-data
  [^java.io.PushbackInputStream s]
  (postwalk (fn [x]
              (if (instance? (clojure.lang.RT/classForName "[B") x)
                (try
                  (apply str (map char x))
                  (catch IllegalArgumentException _ x))
                x))
            (bencode/read-bencode s)))
