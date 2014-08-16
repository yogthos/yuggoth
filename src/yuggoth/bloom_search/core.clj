(ns yuggoth.bloom-search.core
 (:require [clojure.string :as s]
           [com.github.kyleburton.clj-bloom
            :refer [make-optimal-filter
                    optimal-n-and-k
                    make-permuted-hash-fn
                    make-hash-fn-crc32
                    add!
                    include?]])
  (:import [java.util BitSet]))

(def threshold 0.2)

(def filters (atom {}))

(defn get-words [string]
 (-> string
     s/lower-case
     (s/split #"\W")))

(defn add-filter! [id string]
 (let [words (get-words string)
       f     (make-optimal-filter (count words) threshold)]
   (doseq [word words] (add! f word))
   (swap! filters assoc id f)))

(defn contains-words? [words f]
  (every? #{true} (map #(include? f (s/lower-case %)) words)))

(defn search [search-string]
  (let [search-terms (get-words search-string)]
    (reduce
      (fn [matches [id bloom-filter]]
        (if (contains-words? search-terms bloom-filter)
          (conj matches id)
          matches))
      [] @filters)))
