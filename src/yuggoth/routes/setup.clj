(ns yuggoth.routes.setup
  (:require [compojure.core
             :refer [defroutes GET POST]]
            [clojure.set :refer [rename-keys]]
            [noir.util.crypt :as crypt]
            [noir.response :refer [redirect]]
            [yuggoth.config :refer [configured? text]]
            [yuggoth.layout :as layout]
            [yuggoth.db.core :refer [get-admin set-admin!]]
            [yuggoth.db.schema :as schema]
            [yuggoth.config :refer [db save!]]))

(defn error [id]
  (throw (Exception. (text id))))

(defn check-admin [{:keys [admin title admin-pass admin-pass-repeat]}]
  (cond
    (not= admin-pass admin-pass-repeat) (error :pass-mismatch)
    (empty? admin) (error :admin-required)
    (empty? title) (error :blog-title-required)
    :else true))

(defn create-admin [admin]
  (-> admin
      (dissoc :admin-pass-repeat)
      (rename-keys {:admin :handle
                    :admin-pass :pass})
      (update-in [:pass] crypt/encrypt)
      (set-admin!)))

(defn init-blog! [config]
  (try
    (check-admin config)
    (save! (-> config
               (dissoc :admin :admin-pass :admin-pass-repeat)
               (assoc :initialized true)
               (update-in [:locale] keyword)
               (update-in [:port] #(Integer/parseInt %))
               (update-in [:ssl] #(Boolean/parseBoolean %))
               (update-in [:ssl-port] #(Integer/parseInt %))))
    (schema/reset-blog @db)
    (create-admin (select-keys config [:admin :title :admin-pass :admin-pass-repeat]))
    (reset! configured? true)
    (redirect "/")
    (catch Exception ex
      (.printStackTrace ex)
      (layout/render "app.html"
                     {:config config
                      :error (.getMessage
                              (if (instance? java.sql.BatchUpdateException ex)
                                (.getNextException ex)
                                ex))}))))

(defroutes setup-routes
  (POST "/blog-setup" {:keys [params]}
        (if @configured?
          (redirect "/")
          (init-blog! params))))
