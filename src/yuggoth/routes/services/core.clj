(ns yuggoth.routes.services.core
  (:require [compojure.core :as compojure]
            [noir.util.route :refer [restricted]]
            [noir.response :refer [edn status content-type]]
            [yuggoth.db.core :as db]
            [yuggoth.config :refer [locale]]
            [yuggoth.routes.services.posts :refer :all]
            [yuggoth.routes.services.comments :refer :all]
            [yuggoth.routes.services.archives :refer :all]
            [yuggoth.routes.services.auth :refer :all]
            [yuggoth.routes.services.profile :refer :all]
            [yuggoth.routes.services.upload :refer :all]))

;;helpers
(defmacro GET [uri params & body]
  `(compojure/GET ~uri ~params
     (do ~@(butlast body)
       (edn ~(last body)))))

(defmacro GET-restricted [uri params & body]
  `(compojure/GET ~uri ~params
     (restricted
       (do ~@(butlast body)
         (edn ~(last body))))))

(defmacro try-body [body]
  `(try
      ~@(butlast body)
      (edn ~(last body))
      (catch Throwable t#
        (status 400 (edn {:error (.getMessage t#)})))))

(defmacro POST [uri params & body]
  `(compojure/POST ~uri ~params
     (~'try-body ~body)))

(defmacro POST-restricted [uri params & body]
  `(compojure/POST ~uri ~params
     (restricted
       (~'try-body ~body))))

(defmacro POST-restricted-raw [uri params & body]
  `(compojure/POST ~uri ~params
     (restricted
       (try
         ~@(butlast body)
         ~(last body)
         (catch Throwable t#
           (.getMessage t#))))))

(defn auth [req]
  (get-in req [:headers "authorization"]))

(compojure/defroutes service-routes
  (POST "/login" req (login! (auth req)))
  (GET "/logout" [] (logout!))

  (GET "/tag/:tagname" [tagname] (archives-by-date (db/posts-by-tag tagname)))
  (POST-restricted "/update-tags" [tags] (update-tags tags))

  (GET "/latest-post" [] (get-latest-post))
  (GET "/blog-post" [id] (get-post (Integer/parseInt id)))
  (GET "/posts/:count" [count] (get-posts (Integer/parseInt count)))

  (POST-restricted "/toggle-post" [post] (toggle-post! post))
  (POST-restricted "/save-post" [post] (save-post! post))

  (GET "/latest-comments" [] (latest-comments))
  (POST "/comment" [blogid captcha content author] (make-comment! blogid captcha content author))
  (POST-restricted "/delete-comment" [id blogid] (delete-comment! id blogid))

  (GET "/list-files" [] (list-files))
  (compojure/GET "/files/:name" [name] (get-file name))
  (POST-restricted-raw "/upload" [file] (upload-file! file))
  (POST-restricted-raw "/delete-file/:name" [name] (delete-file! name))

  (GET "/profile" [] (profile))
  (POST-restricted "/set-blog-title" {:keys [params]} (set-title! params))
  (POST-restricted "/set-admin-handle" {:keys [params]} (set-handle! params))
  (POST-restricted "/set-admin-email" {:keys [params]} (set-email! params))
  (POST-restricted "/set-admin-details" {:keys [params]} (set-about! params))
  (POST-restricted "/change-admin-password" req (change-password! (auth req) (:params req)))

  (GET "/locale" [] (locale))
  (GET "/tags" [] (db/tags))
  (GET "/archives" [] (get-archives)))
