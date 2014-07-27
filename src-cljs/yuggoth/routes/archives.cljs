(ns yuggoth.routes.archives
  (:require [ajax.core :refer [GET POST]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [text
                     format-title-url
                     format-time
                     parse-time]]))

(defn post-visibility [id public]
  (let [state (atom (if public (text :hide) (text :show)))]
    [:span.submit
      {:onClick #(POST "/archives"
                       {:params {:post-id id
                                 :visible public}
                        :handler (fn [_] (swap! state not))})}
          @state]))

(defn make-list [date items]
  [:div
   date
   [:hr]
   [:ul
    (for [{:keys [id time title public]} (reverse (sort-by :time items))]
      [:li.archive
       [:a {:class "archive" :href (str "/blog/" (format-title-url id title))}
        (str (format-time time "MMMM dd") " - " title)]
       (if (session/get :admin)
         [post-visibility id public])])]])

(defn compare-time [post]
  (format-time (:time post) "yyyy MMMM"))

(defn archives-by-date [archives]
  (->> (session/get :archives)
    (group-by compare-time)
    (vec)
    (sort-by #(parse-time (first %) "yyyy MMMM"))
    (reverse)
    (reduce
      (fn [groups [date items]]
        (conj groups (make-list date items)))
      [:div])))

(defn archives-page []
  (println (session/get :archives))
  (println (->> (session/get :archives)
               (map :time)
               (map #(format-time % "yyyy MMMM"))))
  [:div.post
    [:div.entry-title [:h2 (session/get :entry-title)]]
    [:div.entry-content ]])