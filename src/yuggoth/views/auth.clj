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

(defpage "/create-admin" {:keys [title handle pass pass1 email error]}
  (common/layout
    "Create blog"
    (when error [:h2.error error])
    (form-to [:post "/create-admin"]
             (text-field {:placeholder "Blog title"} "title" title)             
             (util/make-form "handle" "name" handle 
                             "pass"  "password" pass
                             "pass1" "confirm password" pass1
                             "email" "email" email)
             [:span.submit {:tabindex 5} "create"])))

(defn check-admin-fields [admin]
  (cond
    (not= (:pass admin) (:pass1 admin)) "entered passwords do not match"
    (empty? (:handle admin)) "administrator name is required"
    (empty? (:title admin)) "blog title is required"
    :else nil))

(defpage [:post "/create-admin"] admin
  (when (nil? (db/get-admin)) 
    (if-let [error (check-admin-fields admin)] 
      (render "/create-admin" (assoc admin :error error))
      (do
        (db/set-admin (update-in (dissoc admin :pass1) [:pass] crypt/encrypt))
        (resp/redirect "/login")))))

(defpage "/profile" {:keys [pass pass1 pass2 info]}
  (common/layout
    "Profile"
    [:h2 (:handle (session/get :admin))]
    (when info [:h2.info info])
    (form-to [:post "/profile"]
             (util/make-form "pass"  "password" nil
                             "pass1" "new password" nil
                             "pass2" "confirm password" nil)
             [:span.submit {:tabindex 4} "change password"])))

(defn update-admin-pass [admin pass]  
  (let [updated-admin (assoc-in admin [:pass] (crypt/encrypt pass))] 
    (try
      (session/remove! :admin)
      (session/put! :admin updated-admin)
      (db/update-admin updated-admin)      
      "password updated successfully"
      (catch Exception ex (.getMessage ex)))))

(defpage [:post "/profile"] {:keys [pass pass1 pass2]}  
  (let [admin (session/get :admin)] 
    (render "/profile"
            {:info 
             (cond (not (crypt/compare pass (:pass admin))) "wrong password"
                   (not= pass1 pass2) "passwords do not match"
                   :else (update-admin-pass admin pass1))})))