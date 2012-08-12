(ns yuggoth.views.blog
  (:use noir.core hiccup.form hiccup.element hiccup.util)
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
                    (hidden-field "public" (str visible))
                    [:span.submit (if visible "hide" "show")])]
     [:div (form-to [:post "/update-post"]
                    (hidden-field "post-id" post-id)
                    [:span.submit "edit"])]]))


(defn post-nav [id]
  [:div
   (if (> id 1) [:div.leftmost (link-to (str "/blog-previous/" id) "previous")])
   (if (< id (db/last-post-id)) [:div.rightmost (link-to (str "/blog-next/" id) "next")])])

(defn display-public-post [postid next?]
  (resp/redirect 
    (if-let [id (db/get-public-post-id postid next?)]
      (str "/blog/" id)
      "/")))

(defn entry [{:keys [id time title content author public]}]  
  (apply common/layout         
         (if id
           [{:title title :elements (admin-forms id public)}
            [:p#post-time (util/format-time time)]
            (markdown/md-to-html-string content)
            (post-nav id)     
            [:br]
            [:br]
            [:div "tags "
             (for [tag (db/tags-by-post id)]
              [:span.tagon {:id "tag"} tag])]
            
            (comments/get-comments id)            
            (comments/make-comment id)]
           ["Welcome to your new blog" "Nothing here yet..."])))


(defpage "/" []
  (if (db/get-admin)
    (util/cache 
      :home 
      (if-let [post (db/get-last-public-post)] 
        (entry post)
        (common/layout "Welcome to your new blog" "Nothing here yet...")))
    (resp/redirect "/create-admin")))


(defpage "/blog-previous/:postid" {:keys [postid]}
  (display-public-post postid false))

(defpage "/blog-next/:postid" {:keys [postid]}
  (display-public-post postid true))

(defpage "/blog/:postid" {:keys [postid]}  
  (let [id (re-find #"\d+" postid)]
    (util/cache (keyword (str "post-" id)) 
                (entry (db/get-post id)))))


(defn tag-list [& [post-id]]
  (let [post-tags (set (if post-id (db/tags-by-post (Integer/parseInt post-id))))] 
    [:div
     (mapcat (fn [tag]
               (if (contains? post-tags tag)
                 [(hidden-field (str "tag-" tag) tag)
                  [:span.tagon tag]]
                 [(hidden-field (str "tag-" tag))
                  [:span.tagoff tag]]))
             (db/tags))
     (text-field {:placeholder "other"} "tag-custom")]))


(util/private-page [:post "/update-post"] {:keys [post-id error]}
  (let [{:keys [title content public]} (db/get-post post-id)] 
    (common/layout
      "Edit post"
      (when error [:div.error error])
      (form-to [:post "/make-post"]
               (text-field {:tabindex 1} "title" title)
               [:br]
               (text-area {:tabindex 2} "content" content)
               [:br]
               (hidden-field "post-id" post-id)
               (hidden-field "public" (str public))               
               "tags " (tag-list post-id)
               [:br]
               [:span.submit {:tabindex 3} "post"]))))


(util/private-page "/make-post" {:keys [content error]}
  (common/layout
    "New post"
    (when error [:div.error error])
    [:div#output]
    (form-to [:post "/make-post"]
             (text-field {:tabindex 1 :placeholder "Title"} "title")
             [:br]
             (text-area {:tabindex 2} "content" content)
             [:br]
             "tags " (tag-list)
             [:br]
             [:div
              [:div.entry-public "public"]
              (check-box {:tabindex 4} "public" true)                            
              [:div.entry-submit [:span.submit {:tabindex 3} "post"]]])))


(util/private-page [:post "/make-post"] post                   
  (if (not-empty (:title post)) 
    (let [{:keys [post-id title content public]} post
          tags (->> post (filter #(.startsWith (name (first %)) "tag-")) 
                 (map second) 
                 (remove empty?))]        
      (if post-id
        (do
          (db/update-post post-id title content public)
          (db/update-tags post-id tags))
        (db/update-tags 
          (:id (db/store-post title content (:handle (session/get :admin)) public))
          tags))
      
      (resp/redirect (if post-id (str "/blog/" (str post-id "-" (url-encode title))) "/")))
    (render "/make-post" (assoc post :error "post title is required"))))


(util/private-page [:post "/toggle-post"] {:keys [post-id public]}                                      
  (db/post-visible post-id (not (Boolean/parseBoolean public)))
  (resp/redirect (str "/blog/" post-id)))
