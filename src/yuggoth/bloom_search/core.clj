(ns yuggoth.bloom-search.core
 (:require [com.github.kyleburton.clj-bloom
            :refer [make-optimal-filter
                    optimal-n-and-k
                    make-permuted-hash-fn
                    make-hash-fn-crc32
                    add!
                    include?]])
  (:import [java.util BitSet]))

(def threshold 0.1)

(def filters (atom {}))

(defn get-words [string]
 (-> string
     clojure.string/lower-case
     (clojure.string/split #"\W")))

(defn add-filter! [id string]
 (let [words (get-words string)
       f     (make-optimal-filter (count words) threshold)]
   (doseq [word words] (add! f word))
   (swap! filters assoc id f)))

(defn contains-words? [words f]
  ;search for any term
  ;(not-empty (filter (partial include? f) words))
  ;search for every term
  (every? #{true} (map (partial include? f) words)))

(defn search [search-string]
  (let [search-terms (get-words search-string)]
    (reduce
      (fn [matches [id bloom-filter]]
        (if (contains-words? search-terms bloom-filter)
          (conj matches id)
          matches))
      [] @filters)))

(defn long->bitset [value]
  (let [bits (BitSet.)]
    (loop [cur-val value
           index   0]
      (if (pos? cur-val)
        (do
          (when (pos? (mod cur-val 2))
            (.set bits index))
          (recur (unsigned-bit-shift-right cur-val 1) (inc index)))
        bits))))

(defn bitset->long [bits]
  (loop [n 0
         i (.length bits)]
    (if (not= i -1)
      (recur (+ n (if (.get bits i) (bit-shift-left 1 i) 0)) (dec i))
      n)))

(defn serialize []
  (for [[id {:keys [num-bits bitarray insertions]}] @filters]
    {:id (name id)
     :size num-bits
     :bitarray (bitset->long bitarray)
     :insertions @insertions}))

(defn deserialize [filters]
  (reduce
   (fn [m {:keys [id size bitarray insertions]}]
     (let [[_ k] (optimal-n-and-k size threshold)]
       (assoc m (keyword id)
         {:hash-fn (make-permuted-hash-fn make-hash-fn-crc32 (map str (range 0 k)))
          :num-bits size
          :bitarray (long->bitset bitarray)
          :insertions (atom insertions)})))
   {} filters))

;; examples

;; serialize and deserialize and test that stuff still works

;; serialized format will have the following format
;; id -> string
;; size long
;; bitarray long
;; insertions long

;(deserialize (serialize))

;(reset! filters (deserialize (serialize)))
