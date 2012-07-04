(ns yuggoth.views.auth
  (:use hiccup.form noir.core)
  (:require [yuggoth.views.util :as util] 
            [yuggoth.views.common :as common]                       
            [noir.util.crypt :as crypt]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.models.db :as db]))

(defpage "/login" []
  (common/layout
    "Login"
    (form-to [:post "/login"]           
           (text-field {:placeholder "User" :tabindex 1} :handle)
           (password-field {:placeholder "Password" :tabindex 2} :pass)
           [:span.submit {:tabindex 3} "login"])))

(defpage [:post "/login"] {:keys [handle pass]}  
  (if-let [admin (db/get-admin)]        
    (if (and (= handle (:handle admin)) 
             (crypt/compare pass (:pass admin))) 
      (session/put! :admin admin)))  
  (resp/redirect "/"))

(defpage "/logout" []
  (session/clear!)
  (resp/redirect "/"))

(defpage "/create-admin" {:keys [handle pass pass1 email error]}
  (common/layout
    "Create blog"
    (when error [:h2.error error])
    (form-to [:post "/create-admin"]
             (text-field {:placeholder "Blog title"} "title")             
             (util/make-form "handle" "name" handle 
                             "pass"  "password" pass
                             "pass1" "confirm password" pass1
                             "email" "email" email)
             [:span.submit {:tabindex 5} "create"])))

(defn check-admin-fields [admin]
  (connd
    (not= (:pass admin) (:pass1 admin)) "entered passwords do not match"
    (nil? (:handle admin)) "administrator name is required"
    (nil? (:title admin)) "blog title is required"
    :else nil))

(defpage [:post "/create-admin"] admin
  (if-let [error (check-admin-fields admin)] 
    (render "/create-admin" {:error error})
    (do
      (db/set-admin (update-in (dissoc admin :pass1) [:pass] crypt/encrypt))
      (resp/redirect "/login"))))