(ns yuggoth.views.archives
  (:use noir.core hiccup.element)
  (:require [yuggoth.models.db :as db] 
            [yuggoth.views.util :as util]
            [yuggoth.views.common :as common]))

(defn make-list [date items]
  [:div 
   date
   [:hr]
   (into [:ul] 
         (for [{:keys [id time title]} items] 
           [:li.archive 
            (link-to {:class "archive"} 
                     (str "/blog/" id) 
                     (str (util/format-time time) " - " title))]))])

(defpage "/archives" []
  (let [archives (db/get-posts)]    
    (common/layout 
      "Archives"
      (reduce
        (fn [groups [date items]]
          (conj groups (make-list date items)))
        [:div]
        (group-by #(util/format-time (:time %) "MM yyyy") archives)))))



