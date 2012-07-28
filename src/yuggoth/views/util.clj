(ns yuggoth.views.util
  (:use hiccup.form)
  (:require [noir.response :as resp]
            [noir.session :as session]
            [yuggoth.models.db :as db]))

(def cached (agent {}))

(defn make-form [& fields]
  (reduce-kv 
    (fn [table i [id name value]]
      (conj table
            [:tr 
             [:td (label id name)] 
             [:td ((if (.startsWith id "pass") password-field text-field) 
                    {:tabindex (inc i)} id value)]]))
    [:table]
    (vec (partition 3 fields))))

(defn format-time
  ([time] (format-time time "dd MM yyyy"))
  ([time fmt]
    (.format (new java.text.SimpleDateFormat fmt) time)))

(defn get-css []
  (or (not-empty (:style (db/get-admin))) "/css/screen.css"))

(defmacro private-page [& content]
  `(if (session/get :admin)
     (do ~@content)
     (resp/redirect "/")))

(defmacro cache [id content]
  (if (session/get :admin)
    content
    `(let [cached# (get @cached ~id)
           last-updated# (:time cached#)
           cur-time# (.getTime (new java.util.Date))
           cached-content# (:content cached# )]
       (if (or (nil? last-updated#)
               (> (- cur-time# last-updated#) 10000))
         (send cached assoc ~id {:time cur-time# :content ~content}))
       (or cached-content# ~content))))
