(ns yuggoth.util
  (:use hiccup.form clavatar.core [clojure.java.io :only [as-url]])
  (:require [noir.response :as resp]
            [noir.session :as session]
            [yuggoth.models.db :as db]
            [noir.io :as io]
            [markdown.core :as md])
  (:import net.sf.jlue.util.Captcha))

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
  "formats the time using SimpleDateFormat, the default format is
   \"dd MMM, yyyy\" and a custom one can be passed in as the second argument"
  ([time] (format-time time "dd MMM, yyyy"))
  ([time fmt]
    (.format (new java.text.SimpleDateFormat fmt) time)))

(defn parse-time [time-str time-format]
  (.parse 
    (new java.text.SimpleDateFormat 
         (or time-format "yyyy-MM-dd HH:mm:ss.SSS")) 
    time-str))

(defn get-css []
  (or (not-empty (:style (db/get-admin))) "/css/screen.css"))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->> 
    (io/slurp-resource "md" filename)      
    (md/md-to-html-string)))

(defn gravatar-url [email]
  (let [url (gravatar email :default :404)] 
    (try (javax.imageio.ImageIO/read (as-url url))
      url
      (catch Exception ex nil))))

(defn gen-captcha-text []
  (->> #(rand-int 26) (repeatedly 6) (map (partial + 97)) (map char) (apply str)))

(defn gen-captcha []
  (let [text (gen-captcha-text)
        captcha (doto (new Captcha))]
    (session/put! :captcha {:text text :image (.gen captcha text 250 40)})))
