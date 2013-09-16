(ns yuggoth.routes.blog
  (:use compojure.core                 
        noir.util.route
        hiccup.core
        hiccup.form 
        hiccup.element 
        hiccup.util 
        yuggoth.config)
  (:require [markdown.core :as markdown] 
            [yuggoth.views.layout :as layout]
            [yuggoth.util :as util]
            [noir.session :as session]
            [noir.response :as resp]   
            [noir.util.cache :as cache]
            [yuggoth.models.db :as db]
            [yuggoth.routes.comments :as comments]))

(defn post-nav [id]
  [:div
   (if (> id 1)                 [:div.leftmost (link-to (str "/blog-previous/" id) (text :previous))])
   (if (< id (db/last-post-id)) [:div.rightmost (link-to (str "/blog-next/" id) (text :next))])])

(defn display-public-post [postid next?]
  (resp/redirect 
     (if-let [id (db/get-public-post-id postid next?)]
       (str "/blog/" id)
       "/")))

(defn entry [{:keys [id time tease title content author public]}]  
  (apply layout/common         
         (if id
           [{:title title}
            [:p#post-time (util/format-time time)]
            (cache/cache! (str id) (if-not (nil? tease)
                                     (markdown/md-to-html-string (str tease "\n\n" content))
                                     (markdown/md-to-html-string content)))
            (post-nav id)     
            [:br]
            [:br]
            [:div (str (text :tags) " ")
             (for [tag (db/tags-by-post id)]
               [:span.tagon {:id (str "tag-" (:id tag))} (:name tag)])]
            (comments/get-comments id)            
            (comments/comment-form id)]
           [(text :empty-page) (text :nothing-here)])))

(defn tag-list [& [post-id]]
  (let [post-tags (set (if post-id (db/tag-ids-by-post (Integer/parseInt post-id))))] 
    [:div
     (mapcat (fn [tag]
               (if (contains? post-tags (:id tag))
                 [(hidden-field (str "tag-" (:id tag)) (:id tag))
                  [:span.tagon (:name tag)]]
                 [(hidden-field (str "tag-" (:id tag)))
                  [:span.tagoff (:name tag)]]))
             (db/tags))
     (text-field {:placeholder (text :other)} "tag-custom")]))


;; TODO: Modify to optionally show last N posts (sans comments) instead of latest post
(defn home-page []   
  (if (:initialized @blog-config)
    (if-let [post (db/get-last-public-post)] 
      (entry post)
      (layout/common (text :welcome-title) (text :nothing-here)))
    (resp/redirect "/setup-blog")))

(defn about-page []
  (layout/common
   "this is the story of yuggoth... work in progress"))

(defroutes blog-routes   
  (GET "/blog-previous/:postid" [postid] (display-public-post postid false))
  (GET "/blog-next/:postid"     [postid] (display-public-post postid true))
  (GET "/blog/:postid" [postid] 
       (if-let [id (re-find #"\d+" postid)]
         (entry (db/get-post id))
         (resp/redirect "/"))) 
  (GET "/"             []               (home-page)))
