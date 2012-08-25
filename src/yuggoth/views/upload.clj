(ns yuggoth.views.upload
  (:require [yuggoth.models.db :as db]
            [noir.session :as session]
            [yuggoth.views.common :as common]
            [yuggoth.views.util :as util]
            [noir.response :as resp])
  (:use noir.core
        hiccup.core
        hiccup.page
        hiccup.form
        hiccup.element))

(util/private-page "/upload" {:keys [info]}
  (common/layout
    "Upload file"
    [:h2.info info]
    [:div [:h3 "available files"]
     (into [:ul] 
           (for [name (db/list-files)]             
             [:li.file-link (link-to (str "/files/" name) name) 
              [:span "  "] 
              [:div.file               
               (form-to [:post (str "/delete-file/" name)]                                               
                        (submit-button {:class "delete"} "delete"))]]))]
    [:br]
    
    (form-to {:enctype "multipart/form-data"}
             [:post "/upload"]
             (label :file "File to upload")
             (file-upload :file)
             [:br]
             [:span.submit "upload"])))

(util/private-page [:post "/upload"] params
  (render "/upload"
          {:info 
           (try
             (db/store-file (:file params)) 
             "file uploaded successfully"
             (catch Exception ex
               (do
                 (.printStackTrace ex)
                 (str "An error has occured while uploading the file: "
                      (.getMessage ex)))))}))


(util/private-page [:post "/delete-file/:name"] params                   
  (db/delete-file (:name params))
  (resp/redirect "/upload"))

(defpage "/files/:name" {:keys [name]}
  (if-let [{:keys [name type data]} (db/get-file name)]
    (resp/content-type type (new java.io.ByteArrayInputStream data))
    (resp/status 404 "")))
