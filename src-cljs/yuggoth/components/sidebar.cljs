(ns yuggoth.components.sidebar
  (:require [yuggoth.session :as session]
            [yuggoth.util
             :refer [text
                     link
                     fetch-post
                     set-current-post!
                     set-location!
                     nav-link]]))

(defn tag-list []
  (if-let [tags (-> (session/get :tags) sort not-empty)]
    [:div.taglist
     [:h3 "Categories"]
     [:ul
     (for [tag tags]
       ^{:key tag}
       [:li [:div.tag [link (str "#/tag/" tag) [:span.tagon tag]]]])]]))

(defn sidebar []
  (when-not (session/get :mobile?)
    (let [title (session/get :entry-title)]
      (if (or (= (text :new-post) title) (= (text :edit-post) title))
        [:div.sidebar-preview
         [:h2 [:span.render-preview (text :preview-title)]]
         [:div#post-preview]]

        [:div.sidebar
         [:h2 (text :recent-posts-title)]
         [:ul
            (for [{:keys [id time title]} (session/get :recent-posts)]
              ^{:key id}
              [:li [:a {:on-click (fetch-post id
                                     #(do
                                        (set-current-post! %)
                                        (set-location! "#/blog/" (:id %))))}
                    title [:div.date time]]])
            [link "#/archives" (text :more)]]
         [tag-list]]))))
