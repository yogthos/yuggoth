(ns yuggoth.handler
  (:require [yuggoth.routes.blog :refer [blog-routes]]
            [yuggoth.routes.services.core :refer [service-routes]]
            [yuggoth.routes.rss :refer [rss-routes]]
            [yuggoth.routes.setup :refer [setup-routes]]
            [yuggoth.session-manager :as session-manager]
            [yuggoth.config :as config]
            [yuggoth.db.core :as db]
            [yuggoth.middleware :refer [load-middleware]]
            [compojure.core :refer [defroutes]]
            [compojure.route :as route]
            [noir.util.middleware :refer [app-handler]]
            [noir.session :as session]
            [noir.util.cache :as cache]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [cronj.core :as cronj]))

(defroutes base-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (config/init!)
  (reset! config/configured?
          (boolean (db/get-admin)))
  (cache/set-size! 5)
  (if (env :dev) (parser/cache-off!))
  (cronj/start! session-manager/cleanup-job)
  (println "yuggoth started successfully..."))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (println "myapp is shutting down...")
  (cronj/shutdown! session-manager/cleanup-job)
  (println "shutdown complete!"))

(defn admin-page [req]
  (session/get :admin))

(def app (app-handler
          [rss-routes
           blog-routes
           service-routes
           setup-routes
           base-routes]
           :middleware (load-middleware)
           :access-rules [admin-page]
           :formats [:edn]))

