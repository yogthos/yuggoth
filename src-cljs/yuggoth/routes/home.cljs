(ns yuggoth.routes.home
  (:require [ajax.core :refer [GET POST]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [text]]))

(defn toggle-post [content post-id public]
  )

(defn admin-forms [post-id visible]
  (when (session/get :admin)
    [:div
     [:div [:span.submit
            {:on-click #(do
                         (js/alert "toggle")
                         #_(POST "/toggle-post" {:params {:post-id post-id
                                                 :public visible}}))}
            (if visible (text :hide) (text :show))]]

     [:div [:span.submit
            {:on-click #(do
                         (js/alet "edit")
                         #_(POST "/update-post" {:params {:post-id post-id}}))}
            (text :edit)]]]))

(defn home-page []
  [:div.post
    [:div.entry-title [:h2 (session/get :entry-title)]]
    [:div.entry-content "TODO: content"]])
