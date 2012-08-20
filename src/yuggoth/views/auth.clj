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

(defpage "/login" []
  (common/layout
    "Login"
    (form-to [:post "/login"]           
           (text-field {:placeholder "User" :tabindex 1} "handle")
           (password-field {:placeholder "Password" :tabindex 2} "pass")
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

(defpage "/setup-blog" {:keys [host port schema user pass ssl ssl-port error]}
  (if (:initialized @blog-config)
    (resp/redirect "/")
    (html
      [:body 
       [:h2 "Initial Configuration"]
       (if error [:h2.error error])
       (form-to [:post "/setup-blog"]
                (util/make-form "host" "host" host
                                "port" "port" port
                                "schema" "schema" schema
                                "user"   "user" user
                                "pass"   "pass" pass                              
                                "ssl-port" "ssl port" ssl-port)
                (label "ssl" "enable SSL?") (check-box "ssl" false)
                [:br]
                (submit-button "initialize"))])))



(defpage [:post "/setup-blog"] config
  (try
    (write-config (-> config                    
                    (assoc :initialized true)
                    (update-in [:port] #(if (not-empty %) (Integer/parseInt %)))
                    (update-in [:ssl] #(Boolean/parseBoolean %))
                    (update-in [:ssl-port] #(or % 443))))
    (init-config)
    (resp/redirect "/create-admin")
    (catch Exception ex
      (render "/setup-blog" (assoc config :error (.getMessage ex))))))

(defpage "/create-admin" {:keys [title handle pass pass1 email error]}
  (if (:setup @blog-config)
    (do
      (schema/reset-blog @config/db)
      (common/layout
        "Create blog"
        (if error [:h2.error error])
        (form-to [:post "/create-admin"]
                 (text-field {:placeholder "Blog title"} "title" title)             
                 (util/make-form "handle" "name" handle 
                                 "pass"  "password" pass
                                 "pass1" "confirm password" pass1
                                 "email" "email" email)
                 [:span.submit {:tabindex 5} "create"])))
    (resp/redirect "/")))

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
      (render "/create-admin" (assoc admin :error error))
      (do
        (db/set-admin (update-in (dissoc admin :pass1) [:pass] crypt/encrypt))
        (swap! blog-config assoc :setup false)
        (resp/redirect "/login")))))

