(ns yuggoth.views.util
  (:use hiccup.form)
  (:require [noir.response :as resp]
            [noir.session :as session]
            [yuggoth.models.db :as db]))

(def cached (atom {}))

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
  ([time] (format-time time "dd MMM, yyyy"))
  ([time fmt]
    (.format (new java.text.SimpleDateFormat fmt) time)))

(defn get-css []
  (or (not-empty (:style (db/get-admin))) "/css/screen.css"))

(defmacro private-page [path params & content]
  `(noir.core/defpage 
     ~path 
     ~params 
     (if (session/get :admin) (do ~@content) (resp/redirect "/"))))

(defn invalidate-cache [k]
  (swap! cached assoc-in [k :invalid] true))
 
(defmacro cache [id content]
  `(if (session/get :admin)
    ~content
    (let [last-updated# (:time (get @cached ~id))
          invalid#  (:invalid (get @cached ~id))
          cur-time# (.getTime (new java.util.Date))]
      (if (or invalid# 
              (nil? last-updated#)
              (> (- cur-time# last-updated#) 60000))
        (swap! cached assoc ~id {:time cur-time# :content ~content}))
      (:content (get @cached ~id))))) 
