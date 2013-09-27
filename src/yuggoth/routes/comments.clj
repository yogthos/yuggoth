(ns yuggoth.routes.comments
  (:use compojure.core
        hiccup.util
        hiccup.element
        hiccup.form
        noir.util.route
        yuggoth.config)
  (:require [clojure.string :as string]
            [yuggoth.util :as util]
            [yuggoth.models.db :as db]
            [yuggoth.views.layout :as layout]
            [noir.session :as session]
            [noir.request :as request]
            [noir.response :as resp]
            [markdown.core :as markdown])
  (:import javax.imageio.ImageIO
           [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn get-comments [blog-id]
  (sort-by :time (db/get-comments blog-id)))

(defn make-comment [blogid captcha content author]
  (let [admin  (session/get :admin)
        author (or (:handle admin) author)]    
    (if (and (or admin 
                 (and (= captcha (:text (session/get :captcha))) 
                      (not-empty author)))
             (not-empty content))
      (do
        (db/add-comment blogid
                        (string/replace
                          (.. (as-str content)
                            (replace "<" "&lt;")
                            (replace ">" "&gt;"))
                          #"\n&gt;" "\n>")
                        (cond
                          admin author

                          (= (.toLowerCase (:handle (db/get-admin))) (.toLowerCase author))
                          (text :anonymous)

                          :else (escape-html author)))
        (resp/json {:result "success"}))
      (resp/json {:result "error"}))))

(defn display-captcha []
  (util/gen-captcha)
  (resp/content-type 
    "image/jpeg" 
    (let [out (new ByteArrayOutputStream)]
      (ImageIO/write (:image (session/get :captcha)) "jpeg" out)
      (new ByteArrayInputStream (.toByteArray out)))))

(defn delete-comment [id blogid]
  (db/delete-comment id)
  (resp/redirect (str "/blog/" blogid)))

(defn latest-comments []
  (layout/render "latest-comments.html" {:comments (db/get-latest-comments 10)}))

(defroutes comments-routes   
  (POST "/comment"        [blogid captcha content author] (make-comment blogid captcha content author))
  (POST "/delete-comment" [id blogid]                     (restricted (delete-comment id blogid)))
  (GET "/captcha"         []                              (display-captcha))
  (GET "/latest-comments" []                              (latest-comments)))
