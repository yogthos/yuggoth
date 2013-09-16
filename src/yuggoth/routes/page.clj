(ns yuggoth.routes.page
  (:use compojure.core                 
        noir.util.route
        hiccup.core
        hiccup.form 
        hiccup.element 
        hiccup.util 
        yuggoth.config)
  (:require [markdown.core :as markdown] 
            [yuggoth.views.layout :as layout]
            [yuggoth.util :as util]
            [noir.session :as session]
            [noir.util.cache :as cache]
            [yuggoth.models.db :as db]
            [yuggoth.routes.comments :as comments]))

(defn page [{:keys [id title content public]}]  
  (apply layout/common         
         (if id
           [{:title title}
            (cache/cache! (str id) (markdown/md-to-html-string content))
            [:br]
            [:br]]
           [(text :empty-page) (text :nothing-here)])))

(defroutes page-routes   
  (GET "/p/:slug" [slug] (page (db/get-page slug))) )
