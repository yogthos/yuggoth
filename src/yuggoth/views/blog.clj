(ns yuggoth.views.blog
  (:use noir.core hiccup.form)
  (:require markdown
            [yuggoth.views.util :as util]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.views.common :as common]
            [yuggoth.models.db :as db]))

(defn admin-forms [post-id]
  (when (session/get :admin) 
    [:div
     [:div (form-to [:post "/delete-post"]
                    (hidden-field "post-id" post-id)
                    [:span.delete "delete"])]
     [:div (form-to [:post "/update-post"]
                    (hidden-field "post-id" post-id)
                    [:span.submit "edit"])]]))

(defn entry [{:keys [id time title content author]}]
  (apply common/layout
         (if id
           [{:title title :elements (admin-forms id)}
            (markdown/md-to-html-string content)]
           ["Welcome to your new blog" "Nothing here yet..."])))

(defpage "/" []     
  (if (db/get-admin)
    (entry (db/get-last-post))
    (resp/redirect "/create-admin")))

(defpage "/blog/:postid" {:keys [postid]}    
  (entry (db/get-post postid)))

(defpage [:post "/update-post"] {:keys [post-id]}  
  (let [{:keys [title content]} (db/get-post post-id)] 
    (common/layout
      "Edit post"
      (form-to [:post "/save-post"]
               (text-field "title" title)
               [:br]
               (text-area "content" content)
               [:br]
               (hidden-field "post-id" post-id)
               [:span.submit "post"]))))

(defpage "/save-post" []
  (common/layout
    "New post"
    (form-to [:post "/save-post"]
             (text-field {:placeholder "Title"} "title")
             [:br]
             (text-area "content")
             [:br]  
             [:span.submit "post"])))

(defpage [:post "/save-post"] {:keys [post-id title content]}  
  (if post-id
    (db/update-post post-id title content)
    (db/store-post title content (:handle (session/get :admin))))
  (resp/redirect "/"))

(defpage [:post "/delete-post"] {:keys [post-id]}
  (db/delete-post post-id)
  (resp/redirect "/"))
