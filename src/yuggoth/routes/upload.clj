(ns yuggoth.routes.upload
  (:use yuggoth.config
        compojure.core       
        noir.util.route
        hiccup.core
        hiccup.page
        hiccup.form
        hiccup.element)
  (:require [yuggoth.models.db :as db]
            [noir.session :as session]
            [noir.response :as resp]            
            [yuggoth.views.layout :as layout]
            [yuggoth.util :as util]))

(defn upload [info]
  (layout/common
    (text :upload-title)
    [:h2.info info]
    [:div [:h3 (text :available-files)]
     [:ul
      (for [name (db/list-files)]             
        [:li.file-link (link-to (str "/files/" name) name) 
         [:span "  "] 
         [:div.file               
          (form-to [:post (str "/delete-file/" name)]                                               
                   (submit-button {:class "delete"} (text :delete)))]])]]
    [:br]
    
    (form-to {:enctype "multipart/form-data"}
             [:post "/upload"]
             (label :file (text :file-to-upload))
             (file-upload :file)
             [:br]
             [:span.submit "upload"])))

(defn handle-upload [file]
  (upload
    {:info 
     (try
       (db/store-file file) 
       (text :file-uploaded)
       (catch Exception ex
         (do
           (.printStackTrace ex)
           (str (text :error-uploading)
                (.getMessage ex)))))}))

(defroutes upload-routes
  (GET "/files/:name" [name]
       (if-let [{:keys [name type data]} (db/get-file name)]
         (resp/content-type type (new java.io.ByteArrayInputStream data))
         (resp/status 404 "")))
  (restricted GET "/upload" [info] (upload info))
  (restricted POST "/upload" [file] (handle-upload file))  
  (restricted POST "/delete-file/:name" [params]
        (do (db/delete-file (:name params))
            (resp/redirect "/upload"))))
