(ns yuggoth.pages.upload
  (:import goog.net.IframeIo
           goog.net.EventType
           [goog.events EventType])
  (:require [goog.events :as gev]
            [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [GET
                     POST
                     text
                     markdown
                     link
                     set-title!
                     set-current-post!
                     set-location!
                     set-post-url]]))

(defn upload-file! [upload-form-id status]
  (reset! status nil)
  (let [io (IframeIo.)]
    (gev/listen io goog.net.EventType.SUCCESS
                #(do
                   (session/update-in! [:files] conj (.getResponseText io))
                   (reset! status [:span (text :file-uploaded)])))
    (gev/listen io goog.net.EventType.ERROR
                #(reset! status [:span.error (text :error-uploading)]))
    (.setErrorChecker io #(= "error" (.getResponseText io)))
    (.sendFromForm io
                   (.getElementById js/document upload-form-id)
                   (str js/context "/upload"))))

(defn delete-file! [name]
  (POST (str "/delete-file/" name)
        {:handler #(session/update-in! [:files] (fn [files] (remove #{name} files)))}))

(defn delete-component [name]
  (let [status (atom :button)]
    (fn []
      (condp = @status
        :button [:span.button {:on-click #(reset! status :confirm)} (text :delete)]
        :confirm [:div (text :confirm)
                  [:span.button {:on-click #(delete-file! name)} (text :yes)]
                  [:span.button {:on-click #(reset! status :button)} (text :no)]]))))

(defn file-item [name]
  [:tr
   [:td.file-link [:a {:href (str js/context "/files/" name)} name]]
   [:td.file [delete-component name]]])

(defn files-list []
  [:table
    (for [name (session/get :files)] ^{:key name} [file-item name])])

(defn upload-page []
  (set-title! (text :upload))
  (let [status (atom nil)
        form-id "upload-form"]
    (GET "/list-files" {:handler #(session/put! :files %)})
    (fn []
      [:div.contents
        [:div.archives
         [:div.upload
          [:h2 (text :available-files)]
          [files-list]
          [:hr]
          [:form {:id form-id
                  :enc-type "multipart/form-data"
                  :method "POST"}
           [:label {:for "file"} (text :file-to-upload)]
           [:input {:id "file" :name "file" :type "file"}]]
          [:span.button {:on-click #(upload-file! form-id status)} (text :upload)]
          (when @status @status)]]])))

