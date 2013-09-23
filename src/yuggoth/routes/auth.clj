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
            [yuggoth.models.db :as db]
            [clojure.string :as s]))

(defn initialized? []
  (:initialized @blog-config))

(defn initialized-with-admin? []
  (and (:initialized @blog-config) (db/get-admin)))

(defn create-admin-page [admin]
  (layout/render "create-admin.html" admin))

(defn login
  ([params]
    (if (initialized-with-admin?)
      (layout/render "login.html")
      (resp/redirect "/setup-blog")))

  ([handle pass]
    (if-let [admin (db/get-admin)]
      (if (and (= handle (:handle admin))
               (crypt/compare pass (:pass admin)))
        (do (cache/invalidate! :home)
            (session/put! :admin admin))))
    (resp/redirect "/")))

(defn check-admin-fields [{:keys [title handle pass pass1] :as params}]
  (cond
    (not= pass pass1) (text :pass-mismatch)
    (empty? handle) (text :admin-required)
    (empty? title) (text :blog-title-required)
    :else nil))

(defn create-admin [admin]
  (if (db/get-admin)
    (resp/redirect "/")
    (if-let [error (check-admin-fields admin)]
      (create-admin-page (assoc admin :error error))
      (do
        (-> admin (dissoc :pass1) (update-in [:pass] crypt/encrypt) (db/set-admin))
        (resp/redirect "/login")))))

(defn setup-blog-page [params]
  (cond
    (initialized-with-admin?)
    (resp/redirect "/")
    (initialized?)
    (resp/redirect "/create-admin")
    :else
    (layout/render "blog-config.html"
                   (-> params
                       (update-in [:port] #(or % 5432))
                       (update-in [:ssl-port] #(or % 443))))))

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
      (resp/redirect "/create-admin")
      (catch Exception ex
        (setup-blog-page (assoc config :error (.getMessage ex)))))))

(defroutes auth-routes
  (GET "/create-admin"  {params :params}  (create-admin-page params))
  (POST "/create-admin" {params :params}  (create-admin params))
  (GET "/setup-blog"    {params :params} (setup-blog-page params))
  (POST "/setup-blog"   {params :params} (handle-setup-blog params))
  (GET "/login"         {params :params} (login params))
  (POST "/login"        [handle pass]    (login handle pass))
  (GET "/logout" []
       (session/clear!)
       (cache/invalidate! :home)
       (resp/redirect "/")))
