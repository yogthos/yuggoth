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
    
    (let [admin (session/get :admin)]            
      (form-to [:post "/profile"]
               (util/make-form "title"  (text :blog-title)       (or title (:title admin))
                               "handle" (text :name)             (or handle (:handle admin))
                               "style"  (text :css-url)          (or style (:style admin))
                               "email"  (text :email)            (or email (:email admin))                               
                               "pass"   (text :password)         nil
                               "pass1"  (text :new-password)     nil
                               "pass2"  (text :confirm-password) nil)
               
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

(defroutes profile-routes
  (GET "/about"        []               (about))
  (GET "/profile"      {params :params} (restricted (profile params)))
  (POST "/profile"     {params :params} (restricted (update-profile params))))
