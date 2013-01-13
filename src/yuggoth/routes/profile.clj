(ns yuggoth.routes.profile
  (:use compojure.core
        noir.util.route
        hiccup.form
        hiccup.element
        clojure.pprint
        yuggoth.config)
  (:require [yuggoth.views.layout :as layout]
            [yuggoth.util :as util]
            [noir.util.crypt :as crypt]
            [noir.session :as session]            
            [markdown.core :as markdown]
            [yuggoth.models.db :as db]
            [noir.response :as resp]))

(defn about []
  (let [{:keys [about style handle email]} (db/get-admin)]   
    (layout/common
      (str "About " handle)            
      (markdown/md-to-html-string (str about))
      [:p [:b email]])))

(defn profile [{:keys [title handle style email about pass pass1 pass2 info]}]
  (layout/common
    "Profile"
    [:h2.info info]
    (link-to "/export" "export blog")
    [:div (form-to {:enctype "multipart/form-data"}
                   [:post "/import"]             
                   [:div.file-upload (file-upload :file)]
                   [:div.file-upload (submit-button "import blog")])]    
    
    (form-to [:post "/update-tags"]
             (label "tags" "select tags to delete ") 
             (mapcat (fn [tag]
                       [(hidden-field (str "tag-" tag))
                        [:span.tagoff tag]])
                     (db/tags))             
             (submit-button "update tags"))
    
    (let [admin (session/get :admin)]            
      (form-to [:post "/profile"]
               (util/make-form "handle" (text :name) (or handle (:handle admin))
                               "style" (text :css-url) (or style (:style admin))
                               "email" (text :email) (or email (:email admin))                               
                               "pass"  (text :password) nil
                               "pass1" (text :new-password) nil
                               "pass2" (text :confirm-password) nil)
               
               [:h2 (text :about-title)]                             
               (text-area {:id "content" :tabindex 6} "about" (or about (:about admin)))
               [:br]
               [:span.submit {:tabindex 7} (text :update-profile)]))))

(defn get-updated-fields [profile] 
  (let [pass (:pass1 profile)
        updated-profile (select-keys profile [:title :handle :email :style :about])] 
    (if (not-empty pass) (assoc updated-profile :pass (when  (crypt/encrypt pass))) updated-profile)))

(defn update-profile 
  ([params]    
    (let [{:keys [pass pass1 pass2]} params        
          admin (session/get :admin)]     
      (profile
        (assoc params 
               :info
               (cond (nil? pass) (text :admin-pass-required) 
                     (not (crypt/compare pass (:pass admin))) (text :wrong-password)
                     (not= pass1 pass2) (text :pass-mismatch)
                     :else (update-profile admin params))))))
  
  ([admin profile]  
    (let [updated-admin (merge admin (get-updated-fields profile))]    
      (try
        (session/remove! :admin)
        (session/put! :admin updated-admin)
        (db/update-admin updated-admin)      
        (text :profile-updated)
        (catch Exception ex (.getMessage ex))))))



(defn export-blog []
  (resp/content-type 
    "text/plain" 
    (let [buf (new java.io.StringWriter)] 
      (pprint (db/export) buf)
      (.toString buf))))

(defn import-blog [params]
  (db/import-posts (slurp (:tempfile (:file params))))
  (resp/redirect "/profile"))

(defn update-tags [tags]
  (db/delete-tags (map second tags))
  (resp/redirect "/profile"))

(defroutes profile-routes
  (GET "/about" [] (about))
  (restricted GET "/profile" {params :params} (profile params))
  (restricted GET "/export"  [] (export-blog))
  (restricted POST "/import" {params :params} (import-blog params))
  (restricted POST "/profile" {params :params} (update-profile params))  
  (restricted POST "/update-tags" {tags :params} (update-tags tags)))
