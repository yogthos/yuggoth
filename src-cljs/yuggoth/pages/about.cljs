(ns yuggoth.pages.about
  (:require [ajax.core :refer [GET POST]]
            [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [text]]))



(defn about-page []
  [:div.post
    [:div.entry-title [:h2 (session/get :entry-title)]]
    [:div.entry-content "TODO: about"]])
