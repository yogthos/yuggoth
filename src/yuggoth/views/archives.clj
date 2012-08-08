	(ns yuggoth.views.archives
	  (:use noir.core hiccup.element hiccup.form)
	  (:require [yuggoth.models.db :as db]             
              [noir.session :as session]
              [noir.response :as resp]
	            [yuggoth.views.util :as util]
	            [yuggoth.views.common :as common]))
	
	(defn make-list [date items]
	  [:div 
	   date
	   [:hr]
	   (into [:ul] 
	         (for [{:keys [id time title public]} items] 
	           [:li.archive 
	            (link-to {:class "archive"} 
	                     (str "/blog/" id) 
	                     (str (util/format-time time "MMMM dd") " - " title))             
              (if (session/get :admin) 
                (form-to [:post "/archives"]
                         (hidden-field "post-id" id)
                         (hidden-field "visible" (str public))
                         [:span.submit (if public "hide" "show")]))]))])

	
	(defpage "/archives" []
	  (util/cache
     :archives
     (let [archives (db/get-posts nil false (session/get :admin))]
       (common/layout 
         "Archives"
         (reduce
           (fn [groups [date items]]
             (conj groups (make-list date items)))
           [:div]
           (->> archives
             (sort-by :time)
             reverse
             (group-by #(util/format-time (:time %) "yyyy MMMM"))))))))
	
 (defpage [:post "/archives"] {:keys [post-id visible]}   
   (db/post-visible post-id (not (Boolean/parseBoolean visible)))
   (resp/redirect "/archives"))
	
	
