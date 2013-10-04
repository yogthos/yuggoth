(ns yuggoth.routes.admin
  (:use compojure.core
        noir.util.route
        hiccup.core
        hiccup.form 
        hiccup.element
        hiccup.util 
        yuggoth.config)
  (:require [clojure.set :as set]
            [clojure.string :as s]
            [markdown.core :as markdown] 
            [noir.session :as session]
            [noir.response :as resp]
            [noir.util.cache :as cache]
            [yuggoth.views.layout :as layout]
            [yuggoth.util :as util]
            [yuggoth.models.db :as db]
            [yuggoth.routes.blog :as br]
            [yuggoth.routes.comments :as comments]))

(defn admin-list-posts
  "Posts listing in table with links to delete, edit"
  []
  ; TODO - implement delete operation on posts
  (let [posts (into [] (db/admin-get-posts))]
    (layout/admin "Blog Posts" {:link "/admin/post/new" :text (text :new-post)}
     [:table {:class "table table-striped"}
      [:thead
       [:tr
        [:th (clojure.string/capitalize (text :id-header))]
        [:th (clojure.string/capitalize (text :title))]
        [:th (clojure.string/capitalize (text :time-header))]
        [:th (clojure.string/capitalize (text :public-header))]
        [:th (clojure.string/capitalize (text :author))]
        [:th (clojure.string/capitalize (text :actions-header))]]]
      (for [post posts]
        [:tr
         [:td (:id post)]
         [:td (:title post)]
         [:td (:time post)]
         [:td (:public post)]
         [:td (clojure.string/capitalize (:author post))]
         [:td (link-to (str "/admin/post/edit/" (:id post)) "Edit")"&nbsp;"#_(link-to (str "/admin/post/delete/" (:id post)) "Delete")]])])))

(defn admin-list-pages
  "Pages listing in table with links to delete, edit"
  []
  ; TODO - Implement delete operation on pages
  (let [pages (into [] (db/admin-get-pages))]
    (layout/admin "Pages" {:link "/admin/page/new" :text "New Page"}
     [:table {:class "table table-striped"}
      [:thead
       [:tr
        [:th (clojure.string/capitalize (text :id-header))]
        [:th (clojure.string/capitalize (text :title))]
        [:th (clojure.string/capitalize (text :slug-header))]
        [:th (clojure.string/capitalize (text :public-header))]
        [:th (clojure.string/capitalize (text :author))]
        [:th (clojure.string/capitalize (text :actions-header))]]]
      (for [page pages]
        [:tr
         [:td (:id page)]
         [:td (:title page)]
         [:td (:slug page)]
         [:td (:public page)]
         [:td (clojure.string/capitalize (:author page))]
         [:td
          (link-to (str "/admin/page/edit/" (:id page)) "Edit")"&nbsp;"
          #_(link-to (str "/admin/page/delete/" (:id page)) "Delete")]
         ])])))

(defn- tag-delete-form
  [tagid]
  (form-to [:post "/admin/tag/delete"]
           (hidden-field "tagid" tagid)
           (submit-button {:class "btn btn-mini"} (s/capitalize (text :delete))))
  )

(defn admin-list-tags
  "Tagslisting in table with links to delete, edit"
  []
  (let [tags (into [] (db/admin-tags))]
    (layout/admin "Tags" {:link "/admin/tag/new" :text "New Tag"}
     [:table {:class "table table-striped"}
      [:thead
       [:tr
        [:th (clojure.string/capitalize (text :id-header))]
        [:th (clojure.string/capitalize (text :name))]
        [:th (clojure.string/capitalize (text :slug-header))]
        [:th {:colspan 2} (clojure.string/capitalize (text :actions-header))]]]
      (for [tag tags]
        [:tr 
         [:td (:id tag)]
         [:td (:name tag)]
         [:td (:slug tag)]
         [:td {:width "40px"}
          (link-to {:class "btn btn-mini"}
                   (str "/admin/tag/edit/" (:id tag)) (s/capitalize (text :edit)))]
         [:td (tag-delete-form (:id tag))]])])))

(defn admin-tag-list [& [post-id]]
  ;;TODO this should really be done in SQL
  (let [post-tags (set (if (and post-id (not (= post-id :new)))
                         (db/tag-ids-by-post (Integer/parseInt post-id))
                         []))]
    (filter (fn [{:keys [id]}] (contains? post-tags id)) (db/admin-tags))))

(defn admin-edit-post
  "Post edit form, used for creating and editing posts."
  [post-id error]
  (layout/render-blog-page
    (text (if (= post-id :new) :new-post :edit-post))
    "create-post.html"
    {:error error
     :post (if (= post-id :new)
             {:id "new"
              :title ""
              :tease ""
              :time (java.util.Date.)
              :content ""
              :tags (br/tag-list)
              :public false}
             (-> (db/get-post post-id)
               (assoc :tags (br/tag-list post-id))
               (assoc :page-title )))}))

(defn admin-edit-page
  "Page edit form, used for creating and editing pages."
  [page-id error]
  (let [new? (if (= page-id :new) true false)
        {:keys [title content tease public time page slug]}
        (if new?
          {:title "" :tease "" :time "" :page true :slug ""
           :content "" :public false}
          (into {} (db/get-post page-id)))
        page-title (if new? (text :new-page) (text :edit-page))]
    (layout/admin
      page-title
      (when error [:div.error error])
      [:div {:class "row"}
       [:div {:class "span12"}
        [:div {:class "row"}
         [:div {:class "span8"}
          (form-to {:class "form-horizontal"}
                   [:post "/admin/post/save"]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "title" (text :title))
                    [:div {:class "controls"}
                     (text-field {:tabindex 1 :class "input-xxlarge"} "title" title)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "slug" (text :slug-header))
                    [:div {:class "controls"}
                     (text-field {:tabindex 1 :class "input-xxlarge"} "slug" slug)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "content" (text :content))
                    [:div {:class "controls"}
                     (text-area {:tabindex 3 :class "input-xxlarge" :rows 10}
                                "content" content)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "public" (text :public-header))
                    [:div {:class "controls"}
                     (check-box {} "public" public true)]]
                   [:div {:class "control-group" :style "clear:both"}
                    [:div {:class "controls"}
                     (submit-button {:class "btn"} (text :submit))]]
                   (hidden-field "postid" page-id)
                   (hidden-field "page" "true")
                   (hidden-field "public" (str public)))]
         [:div {:class "span4" :align "center"} [:h5 (text :markdown-help)] [:hr]
          #_(markdown-help-block)]]]]
      )))

(defn admin-edit-tag
  "Tag edit form, used for creating and editing tags."
  [tag-id error]
  (let [new? (if (= tag-id :new) true false)
        {:keys [name slug]} (if new?
                              {:name "" :slug ""}
                              (into {} (db/get-tag (Integer/parseInt tag-id))))
        page-title (if new? (text :new-tag) (text :edit-tag))]
    (layout/admin
      page-title
      (when error [:div.error error])
      [:div {:class "row"}
       [:div {:class "span12"}
        [:div {:class "row"}
         [:div {:class "span8"}
          (form-to {:class "form-horizontal"}
                   [:post "/admin/tag/save"]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "name" (s/capitalize (text :name)))
                    [:div {:class "controls"}
                     (text-field {:tabindex 1 :class "input-xxlarge"} "name" name)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "slug" (s/capitalize (text :slug-header)))
                    [:div {:class "controls"}
                     (text-field {:tabindex 2 :class "input-xxlarge" :rows 5}
                                "slug" slug)]]
                   [:div {:class "control-group" :style "clear:both"}
                    [:div {:class "controls"}
                     (submit-button {:class "btn"} (text :submit))]]
                   (hidden-field "tagid" tag-id))]]]])))

(defn admin-save-post
  [{:keys [postid title page tease content publish time page slug] :as p}]
  (let [tags             (filter #(= "tag-" (apply str (take 4 (name (first %))))) p)
        sel_tag_ids      (set (map #(Integer/parseInt (apply str (drop 4 (name (first %))))) tags))
        publish?         (= "publish" publish)
        page?            (= "true" page)]
    (try
      (if (= postid "new")
        (let [id (:id (db/store-post title tease content publish? page slug))]
          (doseq [tag_id sel_tag_ids]
            (db/tag-post id tag_id)))
        (let [postid (Integer/parseInt postid)
              existing_tags (set (db/tag-ids-by-post (Integer/parseInt postid)))
              new_tags      (set/difference sel_tag_ids existing_tags)
              removed_tags  (set/difference existing_tags sel_tag_ids)]
          ;; Update post (blog) record and invalidate its cache entry
          (db/update-post postid title tease content publish? page slug)
          (doseq [removed_tag removed_tags]
            (db/untag-post postid removed_tag))
          (doseq [new_tag new_tags]
            (db/tag-post postid new_tag))
          (cache/invalidate! postid)))
      (resp/redirect "/admin/posts")
      (catch Exception ex
        (admin-edit-post postid (.getMessage ex))))))

(defn admin-save-tag
  [tagid name slug]
  (prn (str "TagID is: " tagid))
  (if (= tagid "new")
    (db/add-tag name slug)
    (db/update-tag tagid name slug))
  (resp/redirect "/admin/tags"))

(defn admin-delete-tag
  [tagid]
  (db/delete-tag tagid)
  (resp/redirect "/admin/tags"))

(defn admin-clear-cache
  []
  (cache/clear!)
  (resp/redirect "/admin/posts"))

(defn upload-page [& [info]]
  (layout/render "upload.html" {:info info}))

(defn handle-upload [file]
  (resp/redirect
    (str "/admin/upload/"
         (try
           (db/store-file file)
           (text :file-uploaded)
           (catch Exception ex
             (.printStackTrace ex)
             (str (text :error-uploading) (.getMessage ex)))))))

;;turns out servlet-context gets hijacked by the context macro
(def-restricted-routes admin-routes
  ; TODO - add route and fn for post delete
  (GET "/admin" [] (resp/redirect "/admin/posts"))
  (GET "/admin/posts" [] (admin-list-posts))
  (GET "/admin/post/new" [] (admin-edit-post :new false))
  (GET "/admin/post/edit/:postid" [postid] (admin-edit-post postid false))
  (GET "/admin/pages" [] (admin-list-pages))
  (GET "/admin/page/new" [] (admin-edit-page :new false))
  (GET "/admin/page/edit/:pageid" [pageid] (admin-edit-page pageid false))
  (GET "/admin/cache/clear" [] (admin-clear-cache))
  ; TODO - add route and fn for tag delete
  (GET "/admin/tags" [] (admin-list-tags))
  (GET "/admin/tag/new" [] (admin-edit-tag :new false))
  (GET "/admin/tag/edit/:tagid" [tagid] (admin-edit-tag tagid false))
  (POST "/admin/post/save" {params :params} (admin-save-post params))
  (POST "/admin/tag/save" [tagid name slug] (admin-save-tag tagid name slug))
  (POST "/admin/tag/delete" [tagid] (admin-delete-tag tagid))
  (GET "/admin/upload"       [] (upload-page))
  (GET "/admin/upload/:info" [info] (restricted (upload-page info)))
  (POST "/admin/upload"      [file] (restricted (handle-upload file)))    
  (GET "/files/:name"  [name]
       (if-let [{:keys [name type data]} (db/get-file name)]
         (resp/content-type type (new java.io.ByteArrayInputStream data))
         (resp/status 404 "")))
  (POST "/admin/delete-file/:name" [params]
        (restricted
          (do (db/delete-file (:name params))
              (resp/redirect "/upload")))))

