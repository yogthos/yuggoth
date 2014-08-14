(ns yuggoth.routes.blog
  (:require [compojure.core :refer [defroutes GET]]
            [yuggoth.layout :as layout]
            [noir.response :refer [redirect]]))

(defn home-page []
  (layout/render "app.html"))

(defroutes blog-routes
  (GET "/" [] (home-page))
  ;;legacy support
  (GET "/about" [] (redirect "/#/about"))
  (GET "/blog/:postid" [postid]
   (redirect (str "/#/blog/" postid))))
