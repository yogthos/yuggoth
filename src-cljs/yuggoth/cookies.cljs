(ns yuggoth.cookies
  (:refer-clojure :exclude [count get keys vals empty? reset!])
  (:require [goog.net.cookies :as cks]
            [cljs.reader :as reader]))

(defn set!
  "sets a cookie, the max-age defaults to -1 for session cookie"
  [k content & [max-age]]
  (.set goog.net.cookies (name k) (pr-str content) (or max-age -1) nil nil true))

(defn get [k & [default]]
  (or
    (when-let [v (.get goog.net.cookies (name k))]
        (reader/read-string v))
    default))

(defn contains-key? [k]
  (.containsKey goog.net.cookies (name k)))

(defn contains-val? [v]
  (.containsValue goog.net.cookies v))

(defn count []
  (.getCount goog.net.cookies))

(defn keys []
  (js->clj (.getKeys goog.net.cookies)))

(defn vals []
  (js->clj (.getValues goog.net.cookies)))


(defn empty? []
  (.isEmpty goog.net.cookies))

(defn remove!
  "removes a cookie"
  [k]
  (.remove goog.net.cookies (name k)))

(defn clear!
  "removes all cookies"
  []
  (.clear goog.net.cookies))
