(ns yuggoth.server
  ;(:use hiccup.middleware)
  (:require [noir.server :as server]
            [noir.core :as core]
            [noir.response :as resp]
            [noir.session :as session]
            [yuggoth.views archives auth blog comments common profile upload]))

;;hack
(defn fix-base-url [handler]
  (fn [request]
    (with-redefs [noir.options/resolve-url 
                  (fn [url] 
                    ;prepend context to the relative URLs
                    (if (.contains url "://") 
                      url (str (:context request) url)))]
      (handler request))))
        
(server/load-views-ns 'yuggoth.views)
;(def handler (wrap-base-url (server/gen-handler {:mode :prod, :ns 'yuggoth})))
(def handler (fix-base-url (server/gen-handler {:mode :prod, :ns 'yuggoth})))


(defmacro pre-route [route]
  `(core/pre-route ~route {} (when-not (session/get :admin) (resp/redirect "/"))))

(pre-route "/upload")
(pre-route "/update-post")
(pre-route "/make-post")
(pre-route "/delete-post")
(pre-route "/delete-file")
(pre-route "/profile")

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode :ns 'yuggoth})))

