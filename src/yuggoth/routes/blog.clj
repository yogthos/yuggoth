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

(defn display-public-post [postid next?]
  (resp/redirect
     (if-let [id (db/get-public-post-id postid next?)]
       (str "/blog/" id)
       "/")))

(defn entry [{:keys [title content tease id] :as post}]
  (layout/render-blog-page
    title
    "post.html"
    {:post
     (assoc post
            :comments (comments/get-comments id)
            :tags     (db/tags-by-post id)
            :has-last (and id (> id 1))
            :has-next (and id (< id (db/last-post-id)))
            :content  (if tease
                        (markdown/md-to-html-string (str tease "\n\n" content))
                        (markdown/md-to-html-string content)))}))

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

(defn home-page []
  (if (:initialized @blog-config)
    (entry (or (db/get-last-public-post)
               {:title   (text :welcome-title)
                :content (text :nothing-here)}))
    (resp/redirect "/setup-blog")))

(defroutes blog-routes
  (GET "/blog-previous/:postid" [postid] (display-public-post postid false))
  (GET "/blog-next/:postid"     [postid] (display-public-post postid true))
  (GET "/blog/:postid"          [postid] (if-let [id (re-find #"\d+" postid)]
                                           (entry (db/get-post id))
                                           (resp/redirect "/"))) 
  (GET "/"                      []      (home-page)))
