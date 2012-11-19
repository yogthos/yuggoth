(ns yuggoth.views.upload
  (:require [yuggoth.models.db :as db]
            [noir.session :as session]
            [yuggoth.views.common :as common]
            [yuggoth.views.util :as util]
            [noir.response :as resp])
  (:use config
        noir.core
        hiccup.core
        hiccup.page
        hiccup.form
        hiccup.element))

(util/private-page "/upload" {:keys [info]}
  (common/layout
    (text :upload-title)
    [:h2.info info]
    [:div [:h3 (text :available-files)]
     (into [:ul] 
           (for [name (db/list-files)]             
             [:li.file-link (link-to (str "/files/" name) name) 
              [:span "  "] 
              [:div.file               
               (form-to [:post (str "/delete-file/" name)]                                               
                        (submit-button {:class "delete"} (text :delete)))]]))]
    [:br]
    
    (form-to {:enctype "multipart/form-data"}
             [:post "/upload"]
             (label :file (text :file-to-upload))
             (file-upload :file)
             [:br]
             [:span.submit "upload"])))

(util/private-page [:post "/upload"] params
  (render "/upload"
          {:info 
           (try
             (db/store-file (:file params)) 
             (text :file-uploaded)
             (catch Exception ex
               (do
                 (.printStackTrace ex)
                 (str (text :error-uploading)
                      (.getMessage ex)))))}))


(util/private-page [:post "/delete-file/:name"] params                   
  (db/delete-file (:name params))
  (util/local-redirect "/upload"))

(defpage "/files/:name" {:keys [name]}
  (if-let [{:keys [name type data]} (db/get-file name)]
    (resp/content-type type (new java.io.ByteArrayInputStream data))
    (resp/status 404 "")))
