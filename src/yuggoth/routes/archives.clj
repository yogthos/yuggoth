(ns yuggoth.routes.archives
  (:use compojure.core hiccup.element hiccup.form hiccup.util yuggoth.config)
  (:require [yuggoth.models.db :as db]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.util :as util]
            [yuggoth.views.layout :as layout]))

(defn compare-time [post]
  (util/format-time (:time post) "yyyy MMMM"))

(defn archives-by-date [archives]
  (->> archives
    (group-by compare-time)
    (vec)
    (sort-by #(util/parse-time (first %) "yyyy MMMM"))
    (reverse)))

(defn archives []
  (layout/render-blog-page
    (text :archives-title)
    "archives.html"
    {:archives (archives-by-date (db/get-posts false false (boolean (session/get :admin))))}))

(defn show-tag [slug]
  (let [tagname (:name (db/tag-by-slug slug))]
    (layout/render
      tagname
      "archives.html"
      {:archives (archives-by-date (db/posts-by-tag-slug slug))})))

(defroutes archive-routes
  (GET "/archives"     []        (archives))
  (GET "/tag/:tagslug" [tagslug] (show-tag tagslug)))
