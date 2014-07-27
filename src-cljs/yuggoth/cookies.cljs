(ns yuggoth.cookies
  (:refer-clojure :exclude [count get keys vals empty? reset!])
  (:require [goog.net.cookies :as cks]
            [cljs.reader :as reader]))

(defn set!
  "sets a cookie, the max-age defaults to -1 for session cookie"
  [k content & [max-age]]
  (.set goog.net.cookies (name k) (pr-str content) (or max-age -1) nil nil true))

(defn- read-value [v]
  (when v
    (reader/read-string v)))

(defn get
  "gets the value at the key, optional default when value is not found"
  [k & [default]]
  (or
    (->> (name k) (.get goog.net.cookies) read-value)
    default))

(defn contains-key?
  "is the key present in the cookies"
  [k]
  (.containsKey goog.net.cookies (name k)))

(defn contains-val?
  "is the value present in the cookies (as string)"
  [v]
  (.containsValue goog.net.cookies v))

(defn count
  "returns the number of cookies"
  []
  (.getCount goog.net.cookies))

(defn keys
  "returns all the keys for the cookies"
  []
  (map keyword (.getKeys goog.net.cookies)))

(defn vals
  "returns cookie values"
  []
  (map read-value (.getValues goog.net.cookies)))


(defn empty?
  "true if no cookies are set"
  []
  (.isEmpty goog.net.cookies))

(defn remove!
  "removes a cookie"
  [k]
  (.remove goog.net.cookies (name k)))

(defn clear!
  "removes all cookies"
  []
  (.clear goog.net.cookies))
