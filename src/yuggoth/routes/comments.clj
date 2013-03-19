(ns yuggoth.routes.comments
  (:use compojure.core
        hiccup.util
        hiccup.element
        hiccup.form
        noir.util.route
        yuggoth.config)
  (:require [clojure.string :as string] 
            [yuggoth.util :as util]
            [yuggoth.models.db :as db]
            [yuggoth.views.layout :as layout]
            [noir.session :as session]
            [noir.request :as request]
            [noir.response :as resp]             
            [markdown.core :as markdown])
  (:import javax.imageio.ImageIO
           [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn append-comment [comments {:keys [id blogid author content time]}]  
  (conj
    comments
    [:div.comment
     [:h4 (util/format-time time) " - " author]
     [:dnv#comment-content 
      (markdown/md-to-html-string content)
      (if (session/get :admin) 
        (form-to [:post "/delete-comment"]
                 (hidden-field "id" id)
                 (hidden-field "blogid" blogid)
                 (submit-button {:class "delete"} (text :delete))))]]))

(defn get-comments [blog-id]
  (let [header [:div.comments [:h2 (text :comments)] [:hr]]]
    (if-let [comments (sort-by :time (db/get-comments blog-id))]
      (conj (reduce append-comment header comments) [:hr])
      header)))

(defn comment-form [blog-id]    
  (form-to [:post "/comment"]
           (hidden-field "blog-id" blog-id)               
           (if-let [admin (:handle (session/get :admin))]
             [:div (text :commenting-as) admin]             
             [:div
              (text-field {:tabindex 2} "author" (text :anonymous))
              [:br]
              [:div#captcha-image (image {:id "captcha-link"} "/captcha")]
              [:div#captcha-text (text-field  {:placeholder (text :captcha) :tabindex 3} "captcha")]]) 
           
           [:br]
           [:p (text :markdown-help) [:span.help (text :help)]]
           
           [:table.mdhelp
             [:tr [:td (text :italics-help)] [:td [:em (text :italics-example)]]]
             [:tr [:td (text :bold-help)] [:td [:b (text :bold-example)]]]
             [:tr [:td (text :strikethrough-help)] [:td [:strike (text :strikethrough-example)]]]
             [:tr [:td (text :link-help)] [:td (link-to "http://http://example.net/" (text :link-example))]]                          
             [:tr [:td (text :superscript-help)] [:td (text :super-example) [:sup (text :script-example)]]]
             [:tr [:td (text :quote-help)] [:td [:blockquote (text :auote-example)] ]]
             [:tr [:td (text :code-help)] [:td [:code (text :code-help)]]]]
           
           (text-area {:id "comment" :placeholder (text :comment) :tabindex 4} "content") [:br]
           [:span.render-comment-preview (text :preview)]
           [:br]
           [:div.comment-preview
            [:p#post-preview ]]
           [:span.submit-comment {:tabindex 5} (text :submit)]))

(defn make-comment [blogid captcha content author]
  (let [admin  (session/get :admin)
        author (or (:handle admin) author)]    
    (if (and (or admin 
                 (and (= captcha (:text (session/get :captcha))) 
                      (not-empty author)))
             (not-empty content))
      
      (do        
        (db/add-comment blogid
                        (string/replace
                          (.. (as-str content)
                            (replace "<" "&lt;")
                            (replace ">" "&gt;"))
                          #"\n&gt;" "\n>")                                                                                  
                        (cond
                          admin author
                          
                          (= (.toLowerCase (:handle (db/get-admin))) (.toLowerCase author))
                          (text :anonymous)
                          
                          :else (escape-html author)))        
        
        (resp/json {:result "success"}))
      (resp/json {:result "error"}))))

(defn display-captcha []
  (util/gen-captcha)
  (resp/content-type 
    "image/jpeg" 
    (let [out (new ByteArrayOutputStream)]
      (ImageIO/write (:image (session/get :captcha)) "jpeg" out)
      (new ByteArrayInputStream (.toByteArray out)))))

(defn latest-comments []
  (layout/common
    (text :latest-comments-title)
    (into [:table] 
        (for [{:keys [blogid time content author]} (db/get-latest-comments 10)]
          [:tr.padded [:td (link-to (str "/blog/" blogid) content " - " author)]]))))

(defroutes comments-routes   
  (POST "/comment" [blogid captcha content author]   
        (make-comment blogid captcha content author))
  (restricted POST "/delete-comment" [id blogid]  
    (do (db/delete-comment id)        
        (resp/redirect (str "/blog/" blogid))))
  (GET "/captcha" [] (display-captcha))
  (GET "/latest-comments" [] (latest-comments)))
