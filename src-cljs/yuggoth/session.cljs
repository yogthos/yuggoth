(ns yuggoth.session
  (:refer-clojure :exclude [get get-in reset! swap!])
  (:require [reagent.core :as reagent :refer [atom]]
            [yuggoth.cookies :as cookies]))

(def state (atom {}))

(declare sync!)

(defn- sync! []
  (when (:_dirty @state)
    (println "syncing ditry state")
    (clojure.core/swap! state dissoc :_dirty)
    (cookies/set! :state @state))
  (js/setTimeout sync! 1000))

(defn init! []
  (if-let [saved-state (cookies/get :state)]
    (clojure.core/reset! state saved-state))
  (sync!))

(defn- dirty! []
  (clojure.core/swap! state assoc :_dirty true))

(defn get
  "Get the key's value from the session, returns nil if it doesn't exist."
  [k & [default]]
  (clojure.core/get @state k default))

(defn put! [k v]
  (clojure.core/swap! state assoc k v :_dirty true))

(defn get-in
 "Gets the value at the path specified by the vector ks from the session,
  returns nil if it doesn't exist."
  [ks & [default]]
  (clojure.core/get-in @state ks default))

(defn swap!
  "Replace the current session's value with the result of executing f with
  the current value and args."
  [f & args]
  (dirty!)
  (apply clojure.core/swap! state f args))

(defn clear!
  "Remove all data from the session and start over cleanly."
  []
  (dirty!)
  (clojure.core/reset! state {}))

(defn reset! [m]
  (dirty!)
  (clojure.core/reset! state m))

(defn remove!
  "Remove a key from the session"
  [k]
  (dirty!)
  (clojure.core/swap! state dissoc k))

(defn assoc-in!
  "Associates a value in the session, where ks is a
   sequence of keys and v is the new value and returns
   a new nested structure. If any levels do not exist,
   hash-maps will be created."
  [ks v]
  (dirty!)
  (clojure.core/swap! state #(assoc-in % ks v)))

(defn get!
  "Destructive get from the session. This returns the current value of the key
  and then removes it from the session."[k & [default]]
  (dirty!)
  (let [cur (get k default)]
    (remove! k)
    cur))

(defn get-in!
  "Destructive get from the session. This returns the current value of the path
  specified by the vector ks and then removes it from the session."
  [ks & [default]]
   (dirty!)
    (let [cur (clojure.core/get-in @state ks default)]
      (assoc-in! ks nil)
      cur))

(defn update-in!
  "'Updates' a value in the session, where ks is a
   sequence of keys and f is a function that will
   take the old value along with any supplied args and return
   the new value. If any levels do not exist, hash-maps
   will be created."
  [ks f & args]
  (dirty!)
  (clojure.core/swap!
    state
    #(apply (partial update-in % ks f) args)))
