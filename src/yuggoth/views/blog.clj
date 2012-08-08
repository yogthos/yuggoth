(ns yuggoth.views.blog
  (:use noir.core hiccup.form hiccup.element)
  (:require markdown
            [yuggoth.views.util :as util]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.views.common :as common]
            [yuggoth.models.db :as db]
            [yuggoth.views.comments :as comments]))

(defn admin-forms [post-id visible]  
  (when (session/get :admin) 
    [:div
     [:div (form-to [:post "/toggle-post"]
                    (hidden-field "post-id" post-id)
                    (hidden-field "visible" (str visible))
                    [:span.submit (if visible "hide" "show")])]
     [:div (form-to [:post "/update-post"]
                    (hidden-field "post-id" post-id)
                    [:span.submit "edit"])]]))


(defn post-nav [id]
  [:div
   (if (> id 1) [:div.leftmost (link-to (str "/blog-previous/" id) "previous")])
   (if (< id (db/last-post-id)) [:div.rightmost (link-to (str "/blog-next/" id) "next")])])


(defn entry [{:keys [id time title content author public]}]  
  (apply common/layout
         (if id
           [{:title title :elements (admin-forms id public)}
            [:p#post-time (util/format-time time)]
            (markdown/md-to-html-string content)
            (post-nav id)     
            [:br]
            ;[:hr]
            
            (comments/get-comments id)            
            (comments/make-comment id)]
           ["Welcome to your new blog" "Nothing here yet..."])))


(defpage "/" []
  (if (db/get-admin)
    (util/cache :home (entry (db/get-last-post)))
    (resp/redirect "/create-admin")))

(defn display-public-post [postid next?]
  (resp/redirect 
    (if-let [id (db/get-public-post-id postid next?)]
      (str "/blog/" id)
      "/")))

(defpage "/blog-previous/:postid" {:keys [postid]}
  (display-public-post postid false))

(defpage "/blog-next/:postid" {:keys [postid]}
  (display-public-post postid true))

(defpage "/blog/:postid" {:keys [postid]}  
  (util/cache (keyword (str "post-" postid)) (entry (db/get-post postid))))


(util/private-page [:post "/update-post"] {:keys [post-id error]}
  (let [{:keys [title content]} (db/get-post post-id)] 
    (common/layout
      "Edit post"
      (when error [:div.error error])
      (form-to [:post "/make-post"]
               (text-field {:tabindex 1} "title" title)
               [:br]
               (text-area {:tabindex 2} "content" content)
               [:br]
               (hidden-field "post-id" post-id)
               [:span.submit {:tabindex 3} "post"]))))


(util/private-page "/make-post" {:keys [content error]}
  (common/layout
    "New post"
    (when error [:div.error error])
    [:div#output]
    (form-to [:post "/make-post"]
             (text-field {:placeholder "Title"} "title")
             [:br]
             (text-area {:onkeypress "render()"} "content" content)
             [:br]
             [:div
              [:div.entry-public "public" ] 
              [:div.entry-public-check (check-box "public" true)]
              [:div.entry-submit [:span.submit {:tabindex 1} "post"]]])))


(util/private-page [:post "/make-post"] post                   
  (if (not-empty (:title post)) 
    (let [{:keys [post-id title content public]} post]
      (if post-id
        (db/update-post post-id title content public)
        (db/store-post title content (:handle (session/get :admin)) public))
      (resp/redirect (if post-id (str "/blog/" post-id) "/")))
    (render "/make-post" (assoc post :error "post title is required"))))


(util/private-page [:post "/toggle-post"] {:keys [post-id visible]}                                      
  (db/post-visible post-id (not (Boolean/parseBoolean visible)))
  (resp/redirect (str "/blog/" post-id)))
