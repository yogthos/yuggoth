(ns yuggoth.handler
  (:use yuggoth.routes.auth
        yuggoth.routes.archives
        yuggoth.routes.blog
        yuggoth.routes.comments
        yuggoth.routes.upload
        yuggoth.routes.profile
        yuggoth.routes.rss
        compojure.core)  
  (:require [yuggoth.config :as config] 
            [noir.util.middleware :as middleware]
            [noir.response :as resp]
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
  (config/init)
  (cache/set-size! 20)
  (println "yuggoth started successfully..."))

(defn private-page [method url params] 
  (session/get :admin))

(defn wrap-ssl-if-selected [app]  
  (if (:ssl @config/blog-config)
    (fn [req]      
      (if (or (not-any? #(= (:uri req) (str (:context req) %)) ["/login"])
              (= :https (:scheme req))
              (= "https" ((:headers req) "x-forwarded-proto")))
        (app req)
        (let [host  (-> req
                        (:headers)
                        (get "host")
                        (clojure.string/split #":")
                        (first))              
              ssl-port (:ssl-port @config/blog-config)]          
          (resp/redirect (str "https://" host ":" ssl-port (:uri req)) :permanent))))    
    app))

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
           (wrap-ssl-if-selected)
           (middleware/wrap-access-rules private-page)))
(def war-handler (middleware/war-handler app))
