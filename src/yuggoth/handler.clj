(ns yuggoth.handler
  (:use yuggoth.routes.auth
        yuggoth.routes.archives
        yuggoth.routes.blog
        yuggoth.routes.comments
        yuggoth.routes.upload
        yuggoth.routes.profile
        yuggoth.routes.rss
        compojure.core)  
  (:require [noir.util.middleware :as middleware]
            [noir.session :as session]
            [noir.util.cache :as cache]
            [compojure.route :as route]))

(defroutes app-routes  
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on 
   an app server such as Tomcat
   put any initialization code here"
  []
  (cache/set-size! 20)
  (println "yuggoth started successfully..."))

(defn private-page [method url params] 
  (session/get :admin))

;;append your application routes to the all-routes vector
(def all-routes [auth-routes
                 archive-routes
                 comments-routes
                 upload-routes
                 profile-routes
                 rss-routes
                 blog-routes                 
                 app-routes])
(def app (-> all-routes
           (middleware/app-handler)
           (middleware/wrap-access-rules private-page)))
(def war-handler (middleware/war-handler app))
