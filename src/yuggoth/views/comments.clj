(ns yuggoth.views.comments
  (:use hiccup.form noir.core)
  (:require [yuggoth.views.util :as util]
            [yuggoth.models.db :as db]
            [noir.response :as resp]))

(defn get-comments [blog-id]
  (vec
    (concat
      [:div.comments [:h2 "comments"] [:hr]] 
      (for [{:keys [author content time]} (db/get-comments blog-id)]
        [:div.comment
         [:h4 (util/format-time time) " - " author]
         [:div#comment-content (markdown/md-to-html-string content)]])
      [[:hr]])))

(defn make-comment [blog-id]  
  (form-to [:post "/comment"]
           (hidden-field "blog-id" blog-id)           
           (text-field {:placeholder "author" :tabindex 2} "author") [:br]
           (text-area {:id "comment" :placeholder "comment" :tabindex 3} "content") [:br]
           (submit-button {:tabindex 4} "submit")
           #_ [:span.submit {:tabindex 4} "submit"]))

(defpage [:post "/comment"] {:keys [blog-id content author]}
  (when (and author content) (db/add-comment blog-id content author))  
  (resp/redirect (str "/blog/" blog-id)))