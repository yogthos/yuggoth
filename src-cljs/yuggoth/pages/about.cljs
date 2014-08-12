(ns yuggoth.pages.about
  (:require [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [GET
                     POST
                     text
                     markdown
                     set-title!]]))

(defn about-page []
  (set-title! (text :about))
  (let [{:keys [about style handle email]} (session/get :profile)]
    [:div.archives
     [:div.entry-title [:h2 (str (text :about-title) " " handle)]]
     [:div.entry-content (markdown about)]
     [:p [:b email]]]))
