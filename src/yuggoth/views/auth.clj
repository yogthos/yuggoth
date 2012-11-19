(ns yuggoth.views.auth
  (:use hiccup.core hiccup.form noir.core config)
  (:require [config :only db]
            [yuggoth.views.locales :as locales]
            [yuggoth.models.schema :as schema]
            [yuggoth.views.util :as util] 
            [yuggoth.views.common :as common]                       
            [noir.util.crypt :as crypt]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.models.db :as db]))

(defn check-admin-fields [admin]
  (cond
    (not= (:pass admin) (:pass1 admin)) (text :pass-mismatch)
    (empty? (:handle admin)) (text :admin-required)
    (empty? (:title admin)) (text :blog-title-required)
    :else nil))

(defpage [:post "/create-admin"] admin  
  (if (db/get-admin) 
    (util/local-redirect "/")
    (if-let [error (check-admin-fields admin)] 
      (render "/login" (assoc admin :error error))
      (do
        (db/set-admin (update-in (dissoc admin :pass1) [:pass] crypt/encrypt))        
        (util/local-redirect "/login")))))

(defn create-admin [{:keys [title handle pass pass1 email]}]
  (form-to [:post "/create-admin"]
           (text-field {:placeholder (text :blog-title)} "title" title)             
           (util/make-form "handle" (text :name) handle 
                           "pass"  (text :password) pass
                           "pass1" (text :confirm-password) pass1
                           "email" (text :email) email)
           [:span.submit {:tabindex 5} (text :create)]))

(defpage "/login" params
  (common/layout
    "Login"
    [:div.error (:error params)]
    (if (db/get-admin)
      (form-to [:post "/login"]           
               (text-field {:placeholder (text :user) :tabindex 1} "handle")
               (password-field {:placeholder (text :password) :tabindex 2} "pass")
               [:span.submit {:tabindex 3} (text :login)])
      (create-admin params))))

(defpage [:post "/login"] {:keys [handle pass]}  
  (if-let [admin (db/get-admin)]        
    (if (and (= handle (:handle admin)) 
             (crypt/compare pass (:pass admin))) 
      (session/put! :admin admin)))  
  (util/local-redirect "/"))

(defpage "/logout" []
  (session/clear!)
  (util/local-redirect "/"))

(defpage "/setup-blog" {:keys [host port schema user pass ssl ssl-port error]}
  (if (:initialized @blog-config)
    (util/local-redirect "/")
    (html
      [:body 
       [:h2 "Initial Configuration"]
       (if error [:h2.error error])
       (form-to [:post "/setup-blog"]
                (util/make-form "host" (text :host) host
                                "port" (text :port) (or port 5432)
                                "schema" (text :schema) schema
                                "user"   (text :user) user
                                "pass"   (text :pass) pass                              
                                "ssl-port" (text :ssl-port) (or ssl-port 443))
                "locale " (drop-down "locale" (keys locales/dict) "en")
                [:br]
                (label "ssl" (text :ssl?)) (check-box "ssl" false)
                [:br]
                (submit-button (text :initialize)))])))

(defpage [:post "/setup-blog"] config  
  (if (:initialized @blog-config)
    (util/local-redirect "/")
    (try 
      (write-config (-> config
                      (assoc :initialized true)
                      (update-in [:locale] keyword)
                      (update-in [:port] #(Integer/parseInt %))
                      (update-in [:ssl] #(Boolean/parseBoolean %))
                      (update-in [:ssl-port] #(Integer/parseInt %))))
      (schema/reset-blog @db)      
      (util/local-redirect "/login")
      (catch Exception ex
        (render "/setup-blog" (assoc config :error (.getMessage ex)))))))

