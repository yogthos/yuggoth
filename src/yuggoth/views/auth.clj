(ns yuggoth.views.auth
  (:use hiccup.core hiccup.form noir.core config)
  (:require [config :only db]
            [yuggoth.models.schema :as schema]
            [yuggoth.views.util :as util] 
            [yuggoth.views.common :as common]                       
            [noir.util.crypt :as crypt]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.models.db :as db]))

(defn check-admin-fields [admin]
  (cond
    (not= (:pass admin) (:pass1 admin)) "entered passwords do not match"
    (empty? (:handle admin)) "administrator name is required"
    (empty? (:title admin)) "blog title is required"
    :else nil))

(defpage [:post "/create-admin"] admin  
  (if (db/get-admin) 
    (resp/redirect "/")
    (if-let [error (check-admin-fields admin)] 
      (render "/login" (assoc admin :error error))
      (do
        (db/set-admin (update-in (dissoc admin :pass1) [:pass] crypt/encrypt))        
        (resp/redirect "/login")))))


(defn create-admin [{:keys [title handle pass pass1 email]}]
  (form-to [:post "/create-admin"]
           (text-field {:placeholder "Blog title"} "title" title)             
           (util/make-form "handle" "name" handle 
                           "pass"  "password" pass
                           "pass1" "confirm password" pass1
                           "email" "email" email)
           [:span.submit {:tabindex 5} "create"]))

(defpage "/login" params
  (common/layout
    "Login"
    [:div.error (:error params)]
    (if (db/get-admin)
      (form-to [:post "/login"]           
               (text-field {:placeholder "User" :tabindex 1} "handle")
               (password-field {:placeholder "Password" :tabindex 2} "pass")
               [:span.submit {:tabindex 3} "login"])
      (create-admin params))))

(defpage [:post "/login"] {:keys [handle pass]}  
  (if-let [admin (db/get-admin)]        
    (if (and (= handle (:handle admin)) 
             (crypt/compare pass (:pass admin))) 
      (session/put! :admin admin)))  
  (resp/redirect "/"))

(defpage "/logout" []
  (session/clear!)
  (resp/redirect "/"))

(defpage "/setup-blog" {:keys [host port schema user pass ssl ssl-port error]}
  (if (:initialized @blog-config)
    (resp/redirect "/")
    (html
      [:body 
       [:h2 "Initial Configuration"]
       (if error [:h2.error error])
       (form-to [:post "/setup-blog"]
                (util/make-form "host" "host" host
                                "port" "port" (or port 5432)
                                "schema" "schema" schema
                                "user"   "user" user
                                "pass"   "pass" pass                              
                                "ssl-port" "ssl port" (or ssl-port 443))
                (label "ssl" "enable SSL?") (check-box "ssl" false)
                [:br]
                (submit-button "initialize"))])))

(defpage [:post "/setup-blog"] config
  (if (:initialized @blog-config)
    (resp/redirect "/")
    (try 
      (write-config (-> config
                      (assoc :initialized true)
                      (update-in [:port] #(Integer/parseInt %))
                      (update-in [:ssl] #(Boolean/parseBoolean %))
                      (update-in [:ssl-port] #(Integer/parseInt %))))
      (schema/reset-blog @db)      
      (resp/redirect "/login")
      (catch Exception ex
        (render "/setup-blog" (assoc config :error (.getMessage ex)))))))

