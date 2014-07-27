(ns yuggoth.routes.archives
  (:require [ajax.core :refer [GET POST]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [text]]))

(defn archives-page []
  [:div.post
    [:div.entry-title [:h2 (session/get :entry-title)]]
    [:div.entry-content "TODO: archives"]])
