(ns yuggoth.views.rss
  (:use noir.core)
  (:require [clojure.xml :as xml]
            [noir.response :as resp]
            [yuggoth.models.db :as db]
            [yuggoth.views.util :as util])
  (:import java.util.Date))

(def site-url "http://yogthos.net/")

(defn gen-item [author {:keys [id title content time]}]
  {:tag :item :attrs nil 
   :content [{:tag :title :attrs nil :content [title]}
             {:tag :dc:creator, :attrs nil, :content [author]}
             {:tag :description :attrs nil 
              :content [(if (> (count content) 300) (str (.substring content 0 300) " [...]") content)]} 
             {:tag :link, :attrs nil, :content [(str site-url "blog/" id )]}               
             {:tag :pubDate, :attrs nil, :content [(util/format-time time "EEE dd MMM yyyy HH:mm:ss ZZZZ")]}
             {:tag :category, :attrs nil, :content ["clojure"]}]})

(defn gen-message [{:keys [title handle]} posts]
  (let [date (util/format-time (new Date) "EEE dd MMM yyyy HH:mm:ss ZZZZ")] 
    {:tag :rss :attrs {:version "2.0"
                       :xmlns:dc "http://purl.org/dc/elements/1.1/"
                       :xmlns:sy "http://purl.org/rss/1.0/modules/syndication/"} 
     :content 
     [{:tag :channel :attrs nil 
       :content (into
                  [{:tag :title :attrs nil :content [(or (:title (first posts)) "")]} 
                   {:tag :description :attrs nil :content [title]} 
                   {:tag :link :attrs nil :content [site-url]} 
                   {:tag :lastBuildDate :attrs nil :content [date]}                                                
                   {:tag :dc:creator, :attrs nil, :content [handle]}
                   {:tag :language, :attrs nil, :content ["en-US"]}
                   {:tag :sy:updatePeriod, :attrs nil, :content ["hourly"]}
                   {:tag :sy:updateFrequency, :attrs nil, :content ["1"]}]
                  (map (partial gen-item handle) posts))}]}))

(defn serve-feed [admin posts]    
  (.getBytes (with-out-str (xml/emit (gen-message admin posts)))))

(defpage "/rss" []
  (resp/content-type 
    "application/rss+xml" 
    (new java.io.ByteArrayInputStream 
         (serve-feed (db/get-admin) (db/get-posts 10 true)))))

