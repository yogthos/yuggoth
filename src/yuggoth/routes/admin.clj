(ns yuggoth.routes.admin
  (:use compojure.core                 
        noir.util.route
        hiccup.core
        hiccup.form 
        hiccup.element 
        hiccup.util 
        yuggoth.config)
  (:require [clojure.set :as set]
            [markdown.core :as markdown] 
            [yuggoth.views.layout :as layout]
            [yuggoth.util :as util]
            [noir.session :as session]
            [noir.response :as resp]   
            [noir.util.cache :as cache]
            [yuggoth.models.db :as db]
            [yuggoth.routes.blog :as br]
            [yuggoth.routes.comments :as comments]))

(defn admin-list-posts
  "Posts listing in table with links to delete, edit"
  []
  (let [posts (into [] (db/admin-get-posts))]
    (layout/admin "Blog Posts" {:link "/admin/post/new" :text "New Post"}
     [:table {:class "table table-striped"}
      [:thead
       [:tr
        [:th "ID"]
        [:th "Title"]
        [:th "Time"]
        [:th "Published?"]
        [:th "Author"]
        [:th "Actions"]]]
      (for [post posts]
        [:tr
         [:td (:id post)]
         [:td (:title post)]
         [:td (:time post)]
         [:td (:public post)]
         [:td (clojure.string/capitalize (:author post))]
         [:td (link-to (str "/admin/post/edit/" (:id post)) "Edit")"&nbsp;"(link-to (str "/admin/post/delete/" (:id post)) "Delete")]])])))

(defn admin-list-pages
  "Pages listing in table with links to delete, edit"
  []
  (let [pages (into [] (db/admin-get-pages))]
    (layout/admin "Pages" {:link "/admin/page/new" :text "New Page"}
     [:table {:class "table table-striped"}
      [:thead
       [:tr
        [:th "ID"]
        [:th "Title"]
        [:th "Slug"]
        [:th "Published?"]
        [:th "Author"]
        [:th "Actions"]]]
      (for [page pages]
        [:tr
         [:td (:id page)]
         [:td (:title page)]
         [:td (:slug page)]
         [:td (:public page)]
         [:td (clojure.string/capitalize (:author page))]
         [:td
          (link-to (str "/admin/page/edit/" (:id page)) "Edit")"&nbsp;"
          (link-to (str "/admin/page/delete/" (:id page)) "Delete")]
         ])])))

(defn admin-list-tags
  "Tagslisting in table with links to delete, edit"
  []
  (let [tags (into [] (db/admin-tags))]
    (layout/admin "Tags" {:link "/admin/tag/new" :text "New Tag"}
     [:table {:class "table table-striped"}
      [:thead
       [:tr
        [:th "ID"]
        [:th "Name"]
        [:th "Slug"]
        [:th "Actions"]]]
      (for [tag tags]
        [:tr
         [:td (:id tag)]
         [:td (:name tag)]
         [:td (:slug tag)]
         [:td (link-to (str "/admin/tag/edit/" (:id tag)) "Edit")"&nbsp;"(link-to (str "/admin/tag/delete/" (:id tag)) "Delete")]])])))

(defn markdown-help-block
  []
  [:table {:class "table table-bordered"}
           [:thead
            [:tr
             [:th "Syntax"]
             [:th "Example"]]]
           [:tr
            [:td (text :italics-help)]
            [:td [:em (text :italics-example)]]]
           [:tr
            [:td (text :bold-help)]
            [:td [:b (text :bold-example)]]]
           [:tr
            [:td (text :strikethrough-help)]
            [:td [:strike (text :strikethrough-example)]]]
           [:tr
            [:td (text :link-help)]
            [:td (link-to "http://http://example.net/" (text :link-example))]]
           [:tr
            [:td (text :superscript-help)]
            [:td (text :super-example) [:sup (text :script-example)]]]
           [:tr
            [:td (text :quote-help)]
            [:td [:blockquote (text :quote-example)] ]]
           [:tr
            [:td (text :code-help)]
            [:td [:code (text :code-help)]]]])

(defn admin-tag-list [& [post-id]]
  (let [post-tags (set (if (and post-id (not (= post-id :new)))
                         (db/tag-ids-by-post (Integer/parseInt post-id))
                         []))]
    [:div {:class "controls"}
     (for [tag (db/admin-tags)]
       (if (contains? post-tags (:id tag))
         [:label {:class "checkbox"} (check-box {} (str "tag-" (:id tag)) true
                                                       (:id tag)) (:name tag)]
         [:label {:class "checkbox"} (check-box {} (str "tag-" (:id tag)) false
                                                       (:id tag)) (:name tag)]
         ))]))

(defn admin-edit-post
  "Post edit form, used for creating and editing posts."
  [post-id error]
  (let [new? (if (= post-id :new) true false)
        {:keys [title content tease public time]} (if new?
                                                    {:title "" :tease "" :time ""
                                                     :content "" :public false}
                                                    (into {} (db/get-post post-id)))
        page-title (if new? "Create Post" "Edit Post")
        tags (if new? (br/tag-list) (br/tag-list post-id))
        pubtime (if new? time (first (clojure.string/split (str time) #" ")))
        #_all-tags ]
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
                    (label {:class "control-label"} "title" "Title")
                    [:div {:class "controls"}
                     (text-field {:tabindex 1 :class "input-xxlarge"} "title" title)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "tease" "Tease")
                    [:div {:class "controls"}
                     (text-area {:tabindex 2 :class "input-xxlarge" :rows 5}
                                "tease" tease)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "content" "Body")
                    [:div {:class "controls"}
                     (text-area {:tabindex 3 :class "input-xxlarge" :rows 10}
                                "content" content)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "public" "Published?")
                    [:div {:class "controls"}
                     (check-box {} "public" public true)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "time" "Publish Date")
                    [:div {:class "controls"}
                     (text-field {:tabindex 5 :class "input-large" :placeholder "yyyy/mm/dd"}
                                 "pubtime" pubtime)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "tags" "Tags")
                    (admin-tag-list post-id)]
                   [:div {:class "control-group" :style "clear:both"}
                    [:div {:class "controls"}
                     (submit-button {:class "btn"} "Submit")]]
                   (hidden-field "postid" post-id)
                   (hidden-field "page" "false")
                   (hidden-field "public" (str public)))]
         [:div {:class "span4" :align "center"} [:h5"Markdown Help"] [:hr]
          (markdown-help-block)]]]]
      )))

(defn admin-edit-page
  "Page edit form, used for creating and editing pages."
  [page-id error]
  (let [new? (if (= page-id :new) true false)
        {:keys [title content tease public time page slug]}
        (if new?
          {:title "" :tease "" :time "" :page true :slug ""
           :content "" :public false}
          (into {} (db/get-post page-id)))
        page-title (if new? "Create Page" "Edit Page")]
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
                    (label {:class "control-label"} "title" "Title")
                    [:div {:class "controls"}
                     (text-field {:tabindex 1 :class "input-xxlarge"} "title" title)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "slug" "Slug")
                    [:div {:class "controls"}
                     (text-field {:tabindex 1 :class "input-xxlarge"} "slug" slug)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "content" "Body")
                    [:div {:class "controls"}
                     (text-area {:tabindex 3 :class "input-xxlarge" :rows 10}
                                "content" content)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "public" "Published?")
                    [:div {:class "controls"}
                     (check-box {} "public" public true)]]
                   [:div {:class "control-group" :style "clear:both"}
                    [:div {:class "controls"}
                     (submit-button {:class "btn"} "Submit")]]
                   (hidden-field "postid" page-id)
                   (hidden-field "page" "true")
                   (hidden-field "public" (str public)))]
         [:div {:class "span4" :align "center"} [:h5"Markdown Help"] [:hr]
          (markdown-help-block)]]]]
      )))

(defn admin-edit-tag
  "Tag edit form, used for creating and editing tags."
  [tag-id error]
  (let [new? (if (= tag-id :new) true false)
        {:keys [name slug]} (if new?
                              {:name "" :slug ""}
                              (into {} (db/get-tag (Integer/parseInt tag-id))))
        page-title (if new? "Create Tag" "Edit Tag")]
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
                    (label {:class "control-label"} "name" "Name")
                    [:div {:class "controls"}
                     (text-field {:tabindex 1 :class "input-xxlarge"} "name" name)]]
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "slug" "Slug")
                    [:div {:class "controls"}
                     (text-field {:tabindex 2 :class "input-xxlarge" :rows 5}
                                "slug" slug)]]
                   [:div {:class "control-group" :style "clear:both"}
                    [:div {:class "controls"}
                     (submit-button {:class "btn"} "Submit")]]
                   (hidden-field "tagid" tag-id))]]]])))

(defn admin-save-post
  [postid title tease content public time page slug p]
  (let [tags (filter #(= "tag-" (apply str (take 4 (name (first %))))) p)
        sel_tag_ids (set (map #(Integer/parseInt (apply str (drop 4 (name (first %))))) tags))
        published? (if-not (nil? public) "true" "false")
        [pubtime _clock] (if-not (nil? time) (clojure.string/split time #" ") "")]
    (if (= postid "new")
      (let [id (:id (db/store-post title tease content pubtime published? page slug))]
        (doseq [tag_id sel_tag_ids]
          (db/tag-post id tag_id)))
      (let [existing_tags (set (db/tag-ids-by-post (Integer/parseInt postid)))
            new_tags (set/difference sel_tag_ids existing_tags)
            removed_tags (set/difference existing_tags sel_tag_ids)]
        ;; Update post (blog) record and invalidate its cache entry
        (db/update-post postid title tease content pubtime published? page slug)
        (doseq [removed_tag removed_tags]
          (db/untag-post (Integer/parseInt postid) removed_tag))
        (doseq [new_tag new_tags]
          (db/tag-post (Integer/parseInt postid) new_tag))
        (cache/invalidate! (str postid))))
    (resp/redirect "/admin/posts"))
  )

(defn admin-save-tag
  [tagid name slug]
  (prn (str "TagID is: " tagid))
  (if (= tagid "new")
    (db/add-tag name slug)
    (db/update-tag tagid name slug))
  (resp/redirect "/admin/tags"))

(defn admin-clear-cache
  []
  (cache/clear!)
  (resp/redirect "/admin/posts"))

(defroutes admin-routes
  ; TODO - add route and fn for post delete
  (GET "/admin" [] (resp/redirect "/admin/posts"))
  (GET "/admin/posts" [] (restricted (admin-list-posts)))
  (GET "/admin/post/new" [] (restricted (admin-edit-post :new false)))
  (GET "/admin/post/edit/:postid" [postid] (restricted (admin-edit-post postid false)))
  (GET "/admin/pages" [] (restricted (admin-list-pages)))
  (GET "/admin/page/new" [] (restricted (admin-edit-page :new false)))
  (GET "/admin/page/edit/:pageid" [pageid] (restricted (admin-edit-page pageid false)))
  (GET "/admin/cache/clear" [] (restricted (admin-clear-cache)))
  ; TODO - add route and fn for tag delete
  (GET "/admin/tags" [] (restricted (admin-list-tags)))
  (GET "/admin/tag/new" [] (restricted (admin-edit-tag :new false)))
  (GET "/admin/tag/edit/:tagid" [tagid] (restricted (admin-edit-tag tagid false)))
  (POST "/admin/post/save" [postid title tease content public pubtime
                            page slug :as {p :params}]
        (restricted (admin-save-post postid title tease content public pubtime page slug p)))
  (POST "/admin/tag/save" [tagid name slug] (restricted (admin-save-tag tagid name slug))))
