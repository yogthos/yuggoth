(ns yuggoth.pages.archives
  (:require [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [yuggoth.pages.home :refer [home-page]]
            [yuggoth.util
             :refer [GET
                     POST
                     text
                     format-title-url
                     fetch-post
                     input-value
                     set-page!
                     set-title!
                     set-current-post!
                     set-location!]]))

(defn post-visibility [id public]
  (let [state (atom public)]
    (fn []
      [:span.submit
        {:on-click
         #(POST "/toggle-post"
                {:params {:post
                          {:id id
                           :public public}}
                 :handler (fn [[result]]
                            (when (pos? result)
                              (swap! state not)))})}
            ({true (text :hide)
              false (text :show)}
             @state)])))

(defn make-list [date items]
  [:div
   date
   [:hr]
   [:ul
    (doall
      (for [{:keys [id time title public]} (reverse (sort-by :time items))]
        ^{:key id}
        [:li.archive
         [:a {:class "archive"
              :on-click
              (fetch-post id
                #(do
                   (set-current-post! %)
                   (set-page! home-page)
                   (set-location! "#/blog/" (:id %))))}
          (str time " - " title)]
         (if (session/get :admin)
           [post-visibility id public])]))]])

(defn submit-search [search-string]
  (cond
   (> (count search-string) 3)
    (POST "/search-posts" {:params {:text search-string}
                           :handler #(session/put! :archives %)})
   (empty? search-string)
   (GET "/archives" {:handler #(session/put! :archives %)})))

(defn search []
  (let [search-string (atom nil)]
    (fn []
      [:div
       [:label (text :search)]
       [:input {:type "text" :on-change #(submit-search (input-value %))}]])))

(defn archives-page []
  (set-title! (text :archives-title))
  [:div.conents
   [:div.archives
    [:div.entry-title
     [:h2 (str (text :archives-title)
               (when-let [tag (session/get :archives-tag)]
                  (str " - " tag)))]]
    [:div.entry-content
     [search]
     [:hr]
     (reduce
      (fn [groups [date items]]
        (conj groups (make-list date items)))
      [:div] (session/get :archives))]]])
