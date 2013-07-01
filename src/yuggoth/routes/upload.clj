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

(defn upload-page [& [info]]
  (layout/common
    (text :upload-title)
    (if info [:h2.info info])
    [:div [:h3 (text :available-files)]
     [:table
      (for [name (db/list-files)]             
        [:tr
         [:td.file-link (link-to (str "/files/" name) name) 
          [:td.file               
           (form-to [:post (str "/delete-file/" name)]                                               
                    (submit-button {:class "delete"} (text :delete)))]]])]]
    [:br]
    
    (form-to {:enctype "multipart/form-data"} [:post "/upload"]
             (label :file (text :file-to-upload))
             (file-upload :file)             
             [:span.submit "upload"])))

(defn handle-upload [file]
  (resp/redirect
    (str "/upload/"
         (try
           (db/store-file file) 
           (text :file-uploaded)
           (catch Exception ex        
             (.printStackTrace ex)
             (str (text :error-uploading) (.getMessage ex)))))))

(defroutes upload-routes
  (GET "/upload"       [] (restricted (upload-page)))
  (GET "/upload/:info" [info] (restricted (upload-page info)))
  (POST "/upload"      [file] (restricted (handle-upload file)))    
  (GET "/files/:name"  [name]
       (if-let [{:keys [name type data]} (db/get-file name)]
         (resp/content-type type (new java.io.ByteArrayInputStream data))
         (resp/status 404 "")))
  (POST "/delete-file/:name" [params]
        (restricted
          (do (db/delete-file (:name params))
              (resp/redirect "/upload")))))
