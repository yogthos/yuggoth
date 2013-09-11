(ns yuggoth.routes.admin
  (:use compojure.core                 
        noir.util.route
        hiccup.core
        hiccup.form 
        hiccup.element 
        hiccup.util 
        yuggoth.config)
  (:require [markdown.core :as markdown] 
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
    (layout/admin "Blog Posts"
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
  (let [post-tags (set (if post-id (db/tag-ids-by-post (Integer/parseInt post-id))))]
    (prn (str "POST-TAGS: " post-tags))
    [:div {:class "controls"}
     (for [tag (db/tags)]
       (if (contains? post-tags (:id tag))
         [:label {:class "checkbox"} (check-box {} (str "tag-" (:id tag)) true
                                                       (:id tag)) (:name tag)]
         [:label {:class "checkbox"} (check-box {} (str "tag-" (:id tag)) false
                                                       (:id tag)) (:name tag)]
         ))
     #_(mapcat (fn [tag]
               (if (contains? post-tags (:id tag))
                 (check-box {} (str "tag-" (:id tag)) true (:id tag))
                 (check-box {} (str "tag-" (:id tag)) false (:id tag))
                 ))
             (db/tags))]
    ))

(defn admin-edit-post
  "Post edit form, used for creating and editing posts."
  [post-id error]
  (let [new? (if (= post-id :new) true false)
        {:keys [title content tease public]} (if new?
                                               {:title "post title" :tease ""
                                                :content "" :public false}
                                               (into {} (db/get-post post-id)))
        page-title (if new? "Create Post" "Edit Post")
        tags (if new? (br/tag-list) (br/tag-list post-id))
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
                   #_(text-field {:tabindex 1} "title" title)
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
                    (label {:class "control-label"} "tags" "Tags")
                    (admin-tag-list post-id)]
                   [:div {:class "control-group" :style "clear:both"}
                    [:div {:class "controls"}
                     (submit-button {:class "btn"} "Submit")]]
                   (hidden-field "postid" post-id)
                   (hidden-field "public" (str public)))]
         [:div {:class "span4" :align "center"} [:h5"Markdown Help"] [:hr]
          (markdown-help-block)]]]]
      )))

(defn admin-save-post
  [postid title tease content public]
  (do
;    (prn (str "PARAMS: " (:params request)))
    (prn (str "POSTID: " postid))
    (if (= postid "new")
      (db/store-post title tease content "Doug" public)
      (do
        ;; Update post (blog) record and invalidate its cache entry
        (db/update-post postid title tease content public)
        (cache/invalidate! (str postid))))
    #_(let [{keys [postid title tease content public]} (:params request)]
        )
    (resp/redirect "/admin/posts"))
  )

(defn admin-clear-cache
  []
  (cache/clear!)
  (resp/redirect "/admin/posts"))

(defroutes admin-routes
  (GET "/admin" [] (resp/redirect "/admin/posts"))
  (GET "/admin/posts" [] (restricted (admin-list-posts)))
  (GET "/admin/post/new" [] (restricted (admin-edit-post :new false)))
  (GET "/admin/post/edit/:postid" [postid] (restricted (admin-edit-post postid false)))
  (GET "/admin/cache/clear" [] (restricted (admin-clear-cache)))
  (POST "/admin/post/save" [postid title tease content public]
        (restricted (admin-save-post postid title tease content public)))
  )
