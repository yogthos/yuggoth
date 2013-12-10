(ns yuggoth.routes.archives
  (:use compojure.core hiccup.element hiccup.form hiccup.util yuggoth.config)
  (:require [yuggoth.models.db :as db]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.util :as util]
            [yuggoth.views.layout :as layout]))

(defn make-list [date items]
  [:div
   date
   [:hr]
   [:ul
    (for [{:keys [id time title public]} (reverse (sort-by :time items))]
      [:li.archive
       (link-to {:class "archive"}
                (str "/blog/" (util/format-title-url id title))
                (str (util/format-time time "MMMM dd") " - " title))
       (if (session/get :admin)
         (form-to [:post "/archives"]
                  (hidden-field "post-id" id)
                  (hidden-field "visible" (str public))
                  [:span.submit (if public (text :hide) (text :show))]))])]])

(defn compare-time [post]
  (util/format-time (:time post) "yyyy MMMM"))

(defn archives-by-date [archives]
  (->> archives
    (group-by compare-time)
    (vec)
    (sort-by #(util/parse-time (first %) "yyyy MMMM"))
    (reverse)
    (reduce
      (fn [groups [date items]]
        (conj groups (make-list date items)))
      [:div])))

(defn archives []
  (layout/common
    (text :archives-title)
    (archives-by-date (db/get-posts false false (boolean (session/get :admin))))))

(defn show-tag [tagname]
  (layout/common
    tagname
    (archives-by-date (db/posts-by-tag tagname))))

(defroutes archive-routes
  (GET "/archives"     []        (archives))
  (GET "/tag/:tagname" [tagname] (show-tag tagname))
  (POST "/archives" [post-id visible]
        (db/post-visible post-id (not (Boolean/parseBoolean visible)))
        (resp/redirect "/archives")))
