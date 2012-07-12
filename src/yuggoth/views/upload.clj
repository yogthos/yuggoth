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

(defpage "/upload" []
  (util/private-page
    (common/layout
      "Upload file"
      [:div [:h3 "available files"]
       (into [:ul] 
             (for [name (db/list-files)]
               [:li.file-link (link-to (str "/files/" name) name) 
                [:span "  "] 
                [:div.file
                 (form-to [:post "/delete-file"]
                          (hidden-field "name" name)                               
                          (submit-button {:class "delete"} "delete"))]]))]
      [:br]
      
      (form-to {:enctype "multipart/form-data"}
               [:post "/upload"]
               (label :file "File to upload")
               (file-upload :file)
               [:br]
               [:span.submit "upload"]))))

(defpage [:post "/upload"] params   
  (util/private-page
    (try
      (db/store-file (:file params))    
      (render "/upload")
      (catch Exception ex
        (do
          (.printStackTrace ex)
          (render "/fail" {:error (.getMessage ex)}))))))


(defpage "/fail" params
  (common/layout
    "File upload failed"
    [:p "An error has occured while uploading the file: " (:error params)]))

(defpage [:post "/delete-file"] params  
  (util/private-page
    (db/delete-file (:name params))
    (resp/redirect "/upload")))

(defpage "/files/:name" {:keys [name]}
  (let [{:keys [name type data]} (db/get-file name)]
    (resp/content-type type (new java.io.ByteArrayInputStream data))))