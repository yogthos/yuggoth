(ns yuggoth.views.rss
  (:use noir.core)
  (:require markdown 
            [clojure.xml :as xml]
            [noir.response :as resp]
            [yuggoth.models.db :as db]
            [yuggoth.views.util :as util])
  (:import java.util.Date))

(def site-url "http://yogthos.net/")

(defmacro tag [id attrs & content]
  `{:tag ~id :attrs ~attrs :content [~@content]})

(defn parse-content [content]
  (.replaceAll
    (.replaceAll 
      (markdown/md-to-html-string (if (> (count content) 300) (str (.substring content 0 500) " [...]") content))
      "<" "&lt;")
    ">" "&gt;"))

(defn gen-item [author {:keys [id title content time]}]
  (tag :item nil 
       (tag :title nil title)
       (tag :dc:creator nil author)
       (tag :description nil (parse-content content))
       (tag :link nil (str site-url "blog/" id ))
       (tag :pubDate nil (util/format-time time "EEE dd MMM yyyy HH:mm:ss ZZZZ"))
       (tag :category nil "clojure")))

(defn gen-message [{:keys [title handle]} posts]
  (let [date (util/format-time (new Date) "EEE dd MMM yyyy HH:mm:ss ZZZZ")] 
    (tag :rss {:version "2.0"
               :xmlns:dc "http://purl.org/dc/elements/1.1/"
               :xmlns:sy "http://purl.org/rss/1.0/modules/syndication/"}
         (update-in 
           (tag :channel nil
                (tag :title nil (or (:title (first posts)) ""))
                (tag :description nil title)
                (tag :link nil site-url)
                (tag :lastBuildDate nil date)
                (tag :dc:creator nil handle)
                (tag :language nil "en-US")
                (tag :sy:updatePeriod nil "hourly")
                (tag :sy:updateFrequency nil "1"))
           [:content]
           into (map (partial gen-item handle) posts)))))

(defn serve-feed [admin posts]
  (.getBytes (with-out-str (xml/emit (gen-message admin posts)))))

(defpage "/rss" []
  (resp/content-type 
    "application/rss+xml" 
    (new java.io.ByteArrayInputStream 
         (serve-feed (db/get-admin) (db/get-posts 10 true)))))
