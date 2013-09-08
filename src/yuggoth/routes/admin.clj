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
  (let [posts (into [] (db/get-posts))]
    (layout/admin "Blog Posts"
     [:table {:class "table table-striped"}
      [:thead
       [:tr
        [:th "ID"]
        [:th "Title"]
        [:th "Time"]
        [:th "Public?"]
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

(defn admin-edit-post
  "Post edit form, used for creating and editing posts."
  [post-id error]
  (let [new? (if (= post-id :new) true false)
        {:keys [title content public]} (if new?
                                         {:title "" :content "" :public false}
                                         (db/get-post post-id))
        page-title (if new? "Create Post" "Edit Post")
        tags (if new? (br/tag-list) (br/tag-list post-id))]
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
                   #_(text-area {:tabindex 2} "content" CONTENT)
                   [:div {:class "control-group"}
                    (label {:class "control-label"} "content" "Body")
                    [:div {:class "controls"}
                     (text-area {:tabindex 2 :class "input-xxlarge" :rows 10}
                                "content" content)]]

                   (hidden-field "post-id" post-id)
                   (hidden-field "public" (str public))               
                   (str :tags " ") tags
                   [:span.submit {:tabindex 3} (text :post)])]
         [:div {:class "span4" :align "center"} [:h5"Markdown Help"] [:hr]
          (markdown-help-block)
          ]]]]
      )))

#_(defn- product-form
  ([title db_id] (product-form title db_id (blank-product)))
  ([title db_id product]
     (let [form_action "/admin/product/save"
           useragency (session/get :useragency)
           agency (agency-from-id useragency)
           carrier_id (:db/id (:product/carrier product))
           product_prodtype (:db/id (:product/prodtype product))
           prod_types (prodtypes-for-agency useragency)
           ptype_vector (prodtypes-as-vector prod_types)]
       (form-to
        {:class "form-horizontal"} [:post form_action]
        (hidden-field :product_type_id db_id )
        (if (= db_id "new")
          (common/btst-drop-down :product_type "Product Type" nil ptype_vector)
          (common/btst-hidden-pair :product_type product_prodtype
                                     "Product Type" "Product Type" ))
        (if (= db_id "new")
          (common/btst-drop-down :carrier "Carrier" nil [])
          (common/btst-hidden-pair :carrier carrier_id
                                   (carrier-name carrier_id useragency)
                                   "Carrier"))
        (common/btst-text-input :product_name (:product/name product)
                                "Product Name" "Enter product name")
        (common/btst-submit-button)))))

(defroutes admin-routes
  (GET "/admin" [] (resp/redirect "/admin/posts"))
  (GET "/admin/posts" [] (restricted (admin-list-posts)))
  (GET "/admin/post/new" [] (restricted (admin-edit-post :new false)))
  (GET "/admin/post/edit/:postid" [postid] (restricted (admin-edit-post postid false)))
;  (POST "/admin/post/save" {post :params} (restricted (save-post post)))
  )
