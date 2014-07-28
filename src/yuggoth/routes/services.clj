(ns yuggoth.routes.services
  (:require [compojure.core :refer :all]
            [noir.util.route :refer [restricted]]
            [noir.response :refer [edn]]
            [noir.session :as session]
            [yuggoth.config :refer [locale blog-config]]
            [yuggoth.locales :refer [dict]]
            [yuggoth.models.db :as db]))

(defroutes service-routes
  (GET "/latest-post" [] (edn (db/get-last-public-post)))
  (GET "/last-post-id" [id] (edn (db/get-public-post-id id false)))
  (GET "/next-post-id" [id] (edn (db/get-public-post-id id true)))
  (GET "/blog-post" [id] (edn (db/get-post id)))


  (GET "/locale" [] (edn (locale)))
  (GET "/tags" [] (edn (db/tags)))
  (GET "/archives" [] (edn (db/get-posts false false (boolean (session/get :admin)))))
  (GET "/posts/:count" [count]
       (edn (db/get-posts (Integer/parseInt count)))))

