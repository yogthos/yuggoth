(ns yuggoth.views.archives
  (:use noir.core hiccup.element)
  (:require [yuggoth.models.db :as db] 
            [yuggoth.views.util :as util]
            [yuggoth.views.common :as common]))

(defpage "/archives" []
  (let [archives (db/get-posts)]    
    (common/layout 
      "Archives"
      (into [:ul] 
            (for [{:keys [id time title]} archives] 
              [:li.archive (link-to (str "/blog/" id)  (str (util/format-time time) " - " title))])))))

