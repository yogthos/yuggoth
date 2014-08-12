(ns yuggoth.pages.latest-comments
  (:require [reagent.core :as reagent :refer [atom]]
            [yuggoth.pages.home :refer [home-page]]
            [yuggoth.session :as session]
            [yuggoth.components.sidebar :refer [sidebar]]
            [yuggoth.util
             :refer [GET
                     POST
                     text
                     markdown
                     set-title!
                     set-page!
                     set-location!]]))

(defn latest-comments-list []
  [:table
     (for [{:keys [blogid time content author]} (session/get :latest-comments)]
       [:tr.padded [:td [:a {:on-click #(do
                                          (set-location! "#/blog/" blogid)
                                          (set-page! home-page))}
                         [:p (markdown content)] " - " author]]])])

(defn latest-comments-page []
  (set-title! (text :latest-comments-title))
  (GET "/latest-comments" {:handler #(session/put! :latest-comments %)})
  (fn []
    [:div.contents
     [:div.post
       [:div.entry-title [:h2 (text :latest-comments-title)]]
       [:div.entry-content
        [latest-comments-list]]]
       [sidebar]]))
