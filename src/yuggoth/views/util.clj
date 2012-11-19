(ns yuggoth.views.util
  (:use hiccup.form clavatar.core [clojure.java.io :only [as-url]])
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

(defn timestamp [time-str]
  (when time-str
    (new java.sql.Timestamp 
         (.getTime (.parse (new java.text.SimpleDateFormat "yyyy-MM-dd HH:mm:ss.SSS") time-str)))))

(defn get-css []
  (or (not-empty (:style (db/get-admin))) "/css/screen.css"))


(defmacro local-redirect [url]
  `(noir.response/redirect 
     (if-let [context# (:context (noir.request/ring-request))]
       (str context# ~url) ~url)))

(defmacro private-page [path params & content]
  `(noir.core/defpage 
     ~path 
     ~params 
     (if (session/get :admin) (do ~@content) (yuggoth.views.util/local-redirect "/"))))

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
               (> (- cur-time# last-updated#) 300000))
         (swap! cached assoc ~id {:time cur-time# :content ~content}))
       (:content (get @cached ~id))))) 

(defn gravatar-url [email]
  (let [url (gravatar email :default :404)] 
    (try (javax.imageio.ImageIO/read (as-url url))
      url
      (catch Exception ex nil))))
  
