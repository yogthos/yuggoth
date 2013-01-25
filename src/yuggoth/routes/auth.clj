(ns yuggoth.routes.auth
  (:use compojure.core hiccup.core hiccup.form yuggoth.config)
  (:require [yuggoth.locales :as locales]
            [yuggoth.models.schema :as schema]
            [yuggoth.util :as util] 
            [yuggoth.views.layout :as layout]                       
            [noir.util.crypt :as crypt]
            [noir.util.cache :as cache]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.models.db :as db]))

(defn create-admin-form [{:keys [title handle pass pass1 email]}]
  (form-to [:post "/create-admin"]
           (text-field {:placeholder (text :blog-title)} "title" title)             
           (util/make-form "handle" (text :name) handle 
                           "pass"  (text :password) pass
                           "pass1" (text :confirm-password) pass1
                           "email" (text :email) email)
           [:span.submit {:tabindex 5} (text :create)]))

(defn login 
  ([params]  
    (layout/common
      "Login"      
      [:div.error (:error params)]
      (if (db/get-admin)
        (form-to [:post "/login"]           
                 (text-field {:placeholder (text :user) :tabindex 1} "handle")
                 (password-field {:placeholder (text :password) :tabindex 2} "pass")
               [:span.submit {:tabindex 3} (text :login)])
        (create-admin-form params))))
  
  ([handle pass]     
    (if-let [admin (db/get-admin)]       
      (if (and (= handle (:handle admin)) 
               (crypt/compare pass (:pass admin))) 
        (do (cache/invalidate! :home)
            (session/put! :admin admin))))  
    (resp/redirect "/")))

(defn check-admin-fields [admin]
  (cond
    (not= (:pass admin) (:pass1 admin)) (text :pass-mismatch)
    (empty? (:handle admin)) (text :admin-required)
    (empty? (:title admin)) (text :blog-title-required)
    :else nil))

(defn create-admin [admin]   
  (if (db/get-admin) 
    (resp/redirect "/")
    (if-let [error (check-admin-fields admin)] 
      (login (assoc admin :error error))
      (do        
        (-> admin (dissoc :pass1) (update-in [:pass] crypt/encrypt) (db/set-admin))        
        (resp/redirect "/login")))))

(defn setup-blog [{:keys [host port schema user pass ssl ssl-port error]}]
  (if (:initialized @blog-config)
    (resp/redirect "/")
    (html
      [:body 
       [:h2 "Initial Configuration"]
       (if error [:h2.error error])
       (form-to [:post "/setup-blog"]
                (util/make-form "host" (text :host) host
                                "port" (text :port) (or port 5432)
                                "schema" (text :schema) schema
                                "user"   (text :user) user
                                "pass"   (text :password) pass                              
                                "ssl-port" (text :ssl-port) (or ssl-port 443))
                "locale " (drop-down "locale" (keys locales/dict) "en")
                [:br]
                (label "ssl" (text :ssl?)) (check-box "ssl" false)
                [:br]
                (submit-button (text :initialize)))])))

(defn handle-setup-blog [config]  
  (if (:initialized @blog-config)
    (resp/redirect "/")
    (try 
      (save (-> config
                      (assoc :initialized true)
                      (update-in [:locale] keyword)
                      (update-in [:port] #(Integer/parseInt %))
                      (update-in [:ssl] #(Boolean/parseBoolean %))
                      (update-in [:ssl-port] #(Integer/parseInt %))))
      (schema/reset-blog @db)      
      (resp/redirect "/login")
      (catch Exception ex
        (setup-blog (assoc config :error (.getMessage ex)))))))

(defroutes auth-routes  
  (POST "/create-admin" {admin :params} (create-admin admin))
  (GET "/setup-blog" {params :params} (setup-blog params))
  (POST "/setup-blog" {config :params} (handle-setup-blog config))
  (GET "/login" {params :params} (login params))
  (POST "/login" [handle pass] (login handle pass))  
  (GET "/logout" [] (do (session/clear!)
                        (cache/invalidate! :home)
                        (resp/redirect "/"))))
