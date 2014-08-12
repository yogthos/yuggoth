(ns yuggoth.routes.services.archives
  (:require [yuggoth.db.core :as db]
            [yuggoth.util :as util]
            [noir.session :as session]))

(defn compare-time [post]
  (util/format-time (:time post) "yyyy MMMM"))

(defn format-times [archives]
  (for [[date posts] archives]
    [date (map #(update-in % [:time] util/format-time) posts)]))

(defn archives-by-date [archives]
  (->> archives
       (group-by compare-time)
       (vec)
       (sort-by #(util/parse-time (first %) "yyyy MMMM"))
       (reverse)
       (format-times)))

(defn get-archives []
  (->> (session/get :admin)
       (boolean)
       (db/get-posts false false)
       (archives-by-date)))
