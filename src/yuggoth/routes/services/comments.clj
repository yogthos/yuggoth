(ns yuggoth.routes.services.comments
  (:require [yuggoth.db.core :as db]
            [noir.session :as session]
            [noir.response :refer [content-type]]
            [hiccup.util :refer [escape-html]]
            [yuggoth.config :refer [text]]
            [yuggoth.util :refer [gen-captcha format-time]])
  (:import javax.imageio.ImageIO
           [java.io ByteArrayOutputStream ByteArrayInputStream]))

(defn valid-comment? [admin author captcha content]
  (and (or admin (not-empty author))
       (not-empty content)))

(defn make-comment! [blogid captcha content author]
  (let [admin  (session/get :admin)
        author (or (:handle admin) author "anonymous")]
    (if (valid-comment? admin author captcha content)
      (db/add-comment! blogid
                       (escape-html content)
                       (cond
                         admin author

                         (= (.toLowerCase (:handle (db/get-admin)))
                            (.toLowerCase author))
                         (text :anonymous)

                         :else (escape-html author)))
      {:result "error" :status "invalid comment"})))

(defn display-captcha []
  (gen-captcha)
  (content-type
    "image/jpeg"
    (let [out (ByteArrayOutputStream.)]
      (ImageIO/write (:image (session/get :captcha)) "jpeg" out)
      (ByteArrayInputStream. (.toByteArray out)))))

(defn delete-comment! [id blogid]
  (db/delete-comment! id)
  {:result "ok"})

(defn latest-comments []
  (db/get-latest-comments 10))
