(ns yuggoth.routes.services.archives
  (:require [yuggoth.db.core :as db]
            [yuggoth.util :as util]
            [yuggoth.bloom-search.core :as bloom]
            [throttler.core :refer [throttle-chan throttle-fn]]
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

(def search
  (throttle-fn
   (fn [text]
     (let [result (-> text bloom/search)]
       (archives-by-date
        (if (session/get :admin)
          result
          (filter #(:public %) result)))))
   10 :second))

(defn index-posts! []
  (reset! bloom/filters {})
  (doseq [post (db/get-posts -1 true true)]
    (bloom/add-filter! (dissoc post :content)
                       (:content post))))
