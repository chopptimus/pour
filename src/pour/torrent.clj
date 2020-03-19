(ns pour.torrent
  (:require [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]]
            [bencode.core :as bencode]))

(defn- byte-array?
  [x]
  (if (nil? x)
    false
    (= (Class/forName "[B")
       (.getClass x))))

(defn torrent-info
  [f]
  (with-open [s (java.io.PushbackInputStream. (io/input-stream f))]
    (postwalk (fn [x]
                (if (byte-array? x)
                  (try
                    (apply str (map char x))
                    (catch IllegalArgumentException ex x))
                  x))
              (bencode/read-bencode s))))
