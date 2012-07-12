(ns yuggoth.server
  ;(:use hiccup.middleware)
  (:require [noir.server :as server]
            [noir.core :as core]
            [noir.response :as resp]
            [noir.session :as session]
            [yuggoth.views archives auth blog comments common profile upload])
  (:gen-class))

;;hack
(defn fix-base-url [handler]
  (fn [request]
    (with-redefs [noir.options/resolve-url 
                  (fn [url] 
                    ;prepend context to the relative URLs
                    (if (.contains url "://") 
                      url (str (:context request) url)))]
      (handler request))))

;;
(defn secure-login-redirect [handler]
  (fn [request]
    (let [{:keys [scheme uri server-name server-port]} request]            
      (if (and (= scheme :http) (.contains uri "login"))      
        (ring.util.response/redirect (str "https://" server-name ":" port  uri))
        (handler request)))))


(server/load-views-ns 'yuggoth.views)
;(def handler (wrap-base-url (server/gen-handler {:mode :prod, :ns 'yuggoth})))
;(def handler (ssl-redirects (server/gen-handler {:mode :prod, :ns 'yuggoth})))
(def handler
  (-> (server/gen-handler 
        {:mode :prod, 
         :ns 'yuggoth 
         :session-cookie-attrs {:max-age 1800000}})
   
    ;;enable this to redirect login to HTTPS 
    ;;make sure that the container has an HTTPS listener setup and that HTTP listener forwards SSL requests to it
    
    ;;on Glassfish: Configurations->Network Config->Network Listeners->http-listener-1->HTTP 
    ;;set redirect port to that of http-listener-2
    
    ;;on Tomcat check server.xml to make sure that HTTP listener has redirectPort set correctly (default should be correct)
    
    ;secure-login-redirect 
    fix-base-url))


(defmacro pre-route [route]
  `(core/pre-route ~route {} (when-not (session/get :admin) (resp/redirect "/"))))

;;does not work when a context is present
;(pre-route "/upload")
;(pre-route "/update-post")
;(pre-route "/make-post")
;(pre-route "/delete-post")
;(pre-route "/delete-file")
;(pre-route "/profile")
;(pre-route "/export")

(defn parse-args [args]
  (into {} 
        (for [[name val] (partition 2 args)]
          (condp = name
            "-port" [:port (Integer/parseInt val)]
            "-mode" [:mode (if (some #{"dev" "prod"} [val])                             
                             (keyword val)
                             (throw (new Exception (str "unkown mode" val))))]            
             (throw (new Exception (str "invalid option " name val " see -help for valid options")))))))

(defn -main [& args]  
  (if (= "-help" (first args))
    (println "valid options:\n-port integer\n-mode dev/prod\n-help this message")
    (let [m (parse-args args)
          mode (get m :mode :dev)         
          port (get m :port (new Integer 8080))]
      (println "starting in mode" mode " on port " port)
      (server/start port {:mode mode :ns 'yuggoth :session-cookie-attrs {:max-age 1800000}}))))
