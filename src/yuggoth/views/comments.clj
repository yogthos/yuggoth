(ns yuggoth.views.comments
  (:use hiccup.form noir.core)
  (:require [yuggoth.views.util :as util]
            [yuggoth.models.db :as db]))

(defn make-comment []
  (form-to [:post "/comment"]
           ))

(defpage [:post "/comment"] {:keys [blog-id title content author]}
  (db/add-comment blog-id title content author))