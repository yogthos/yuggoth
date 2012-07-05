(ns yuggoth.views.profile
  (:use hiccup.form noir.core)
  (:require [yuggoth.views.common :as common]
            [yuggoth.views.util :as util]
            [noir.util.crypt :as crypt]
            [noir.session :as session]
            [yuggoth.models.db :as db]))

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